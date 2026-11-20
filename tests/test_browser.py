import threading
import unittest
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from playwright.sync_api import (
    Browser,
    BrowserContext,
    Page,
    Playwright,
    expect,
    sync_playwright,
)

BUILD = Path(__file__).resolve().parents[1] / "build"


class BrowserTest(unittest.TestCase):
    server: ThreadingHTTPServer
    thread: threading.Thread
    playwright: Playwright
    browser: Browser
    context: BrowserContext
    page: Page

    @classmethod
    def setUpClass(cls) -> None:
        if not (BUILD / "site/index.html").is_file():
            raise RuntimeError("Build the site before running browser tests.")
        # Serve from a subdirectory to match a GitHub project Pages URL.
        cls.server = ThreadingHTTPServer(
            ("127.0.0.1", 0), partial(SimpleHTTPRequestHandler, directory=BUILD)
        )
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.addClassCleanup(cls.server.server_close)
        cls.addClassCleanup(cls.thread.join, 5)
        cls.addClassCleanup(cls.server.shutdown)
        cls.playwright = sync_playwright().start()
        cls.addClassCleanup(cls.playwright.stop)
        cls.browser = cls.playwright.chromium.launch()
        cls.addClassCleanup(cls.browser.close)

    def setUp(self) -> None:
        self.context = self.browser.new_context()
        self.addCleanup(self.context.close)
        self.page = self.context.new_page()
        self.base_url = f"http://127.0.0.1:{self.server.server_port}/site/"

    def test_policy_batch_statistics_and_input_validation(self) -> None:
        self.page.goto(self.base_url)
        simulate = self.page.get_by_role("button", name="Simulate", exact=True)
        expect(simulate).to_be_enabled(timeout=45_000)
        expect(self.page.locator("#expected")).to_have_text("23.24")
        expect(self.page.locator("#last")).to_have_text("29")
        expect(self.page.locator("#count")).to_have_text("1")
        self.page.get_by_role("button", name="Simulate 100", exact=True).click()
        expect(self.page.locator("#count")).to_have_text("101")
        self.page.get_by_role("button", name="View stats", exact=True).click()
        expect(self.page.locator("#histogram")).to_be_visible()
        expect(self.page.locator("#mean")).not_to_have_text("—")

        self.page.get_by_text("Sailing times and wind").click()
        wind = self.page.get_by_label("N left", exact=True)
        wind.fill("0.5")
        calculate = self.page.get_by_role("button", name="Calculate")
        expect(simulate).to_be_disabled()
        calculate.click()
        expect(self.page.get_by_role("alert")).to_contain_text("must add up to 1")
        wind.fill("0.3")
        self.page.get_by_label("Start x").fill("4")
        self.page.get_by_label("Start y").fill("4")
        calculate.click()
        expect(self.page.locator("#expected")).to_have_text("0")
        expect(self.page.locator("#count")).to_have_text("1")
        self.page.set_viewport_size({"width": 390, "height": 844})
        self.assertTrue(
            self.page.evaluate("document.documentElement.scrollWidth <= innerWidth")
        )

    def test_runtime_failure_shows_retry(self) -> None:
        self.page.route("**/vendor/pyodide.js", lambda route: route.abort())
        self.page.goto(self.base_url)
        expect(self.page.get_by_role("alert")).to_contain_text("Python could not load")
        expect(self.page.get_by_role("button", name="Reload")).to_be_visible()
