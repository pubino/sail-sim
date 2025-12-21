# Sailing Simulator Makefile
#
# Targets:
#   make          - Build JAR file (default)
#   make jar      - Build JAR file
#   make app      - Build macOS .app bundle (unsigned)
#   make sign     - Sign and notarize for distribution
#   make run      - Build and run the JAR
#   make run-app  - Build and run the .app bundle
#   make clean    - Remove build artifacts
#   make distclean - Remove all generated files including dist
#
# Requirements:
#   - Java 14+ (for jpackage)
#   - For signing: Apple Developer certificate and notarization credentials

SHELL := /bin/bash

# Directories
PROJECT_DIR := $(shell pwd)
BUILD_DIR := $(PROJECT_DIR)/build
DIST_DIR := $(PROJECT_DIR)/dist
SCRIPTS_DIR := $(PROJECT_DIR)/scripts

# Java settings
JAVAC := javac
JAVA := java
JAR := jar
JPACKAGE := jpackage
JAVAC_FLAGS := -Xlint:-deprecation -Xlint:-removal

# Output files
JAR_FILE := $(DIST_DIR)/SailingSim.jar
APP_BUNDLE := $(DIST_DIR)/SailingSimulator.app
MAIN_CLASS := sail.Sail

# Source files
SOURCES := \
	sail/Sail.java \
	myutil/GL.java \
	myutil/SL.java \
	myutil/Format.java

# Resource files
RESOURCES := \
	sail/beach.gif \
	sail/princeton_shield_small.gif \
	sail/sailboat_right.gif

# App bundle settings
APP_NAME := SailingSimulator
DISPLAY_NAME := Sailing Simulator
BUNDLE_ID := edu.princeton.vanderbei.sail-sim
VERSION := 1.0.0

# Default target
.PHONY: all
all: jar

# Compile Java sources and create JAR
.PHONY: jar
jar: $(JAR_FILE)

$(JAR_FILE): $(SOURCES) $(RESOURCES)
	@echo "=== Compiling Java sources ==="
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)
	$(JAVAC) -d $(BUILD_DIR) -sourcepath . $(JAVAC_FLAGS) $(SOURCES)
	@echo "=== Copying resources ==="
	@mkdir -p $(BUILD_DIR)/sail
	@cp -f sail/*.gif $(BUILD_DIR)/sail/ 2>/dev/null || true
	@echo "=== Creating JAR file ==="
	cd $(BUILD_DIR) && $(JAR) cfe $(JAR_FILE) $(MAIN_CLASS) sail/ myutil/
	@echo "Build complete: $(JAR_FILE)"
	@read -p "Would you like to run the application? [y/N] " answer; \
	if [ "$$answer" = "y" ] || [ "$$answer" = "Y" ]; then \
		if command -v java >/dev/null 2>&1; then \
			echo "=== Running $(JAR_FILE) ==="; \
			java -jar $(JAR_FILE); \
		else \
			echo "Error: Java not found in PATH"; \
			echo ""; \
			echo "To install Java via Homebrew:"; \
			echo "  brew install openjdk"; \
			echo ""; \
			echo "Or install a specific version:"; \
			echo "  brew install openjdk@21"; \
			echo ""; \
			echo "After installation, you may need to add it to your PATH:"; \
			echo "  echo 'export PATH=\"/opt/homebrew/opt/openjdk/bin:\$$PATH\"' >> ~/.zshrc"; \
		fi \
	fi

# Build macOS .app bundle (unsigned)
.PHONY: app
app: $(APP_BUNDLE)

$(APP_BUNDLE): $(JAR_FILE)
	@echo "=== Creating macOS App Bundle ==="
	@rm -rf $(APP_BUNDLE)
	$(JPACKAGE) \
		--type app-image \
		--name "$(APP_NAME)" \
		--input $(DIST_DIR) \
		--main-jar SailingSim.jar \
		--main-class $(MAIN_CLASS) \
		--dest $(DIST_DIR) \
		--app-version $(VERSION) \
		--vendor "Vanderbei" \
		--mac-package-identifier $(BUNDLE_ID) \
		--mac-package-name "$(DISPLAY_NAME)" \
		--java-options "-Xmx512m"
	@echo "App bundle created: $(APP_BUNDLE)"
	@read -p "Would you like to run the application? [y/N] " answer; \
	if [ "$$answer" = "y" ] || [ "$$answer" = "Y" ]; then \
		echo "=== Running $(APP_BUNDLE) ==="; \
		open $(APP_BUNDLE); \
	fi

# Sign and notarize for distribution
.PHONY: sign
sign: app
	@echo "=== Signing and Notarizing ==="
	@if [ -x "$(SCRIPTS_DIR)/sign-and-notarize.sh" ]; then \
		$(SCRIPTS_DIR)/sign-and-notarize.sh; \
	else \
		echo "Error: sign-and-notarize.sh not found or not executable"; \
		exit 1; \
	fi

# Run the JAR
.PHONY: run
run: jar
	@echo "=== Running Sailing Simulator ==="
	$(JAVA) -jar $(JAR_FILE)

# Run the app bundle
.PHONY: run-app
run-app: app
	@echo "=== Running Sailing Simulator App ==="
	open $(APP_BUNDLE)

# Clean build artifacts
.PHONY: clean
clean:
	@echo "=== Cleaning build artifacts ==="
	rm -rf $(BUILD_DIR)
	rm -f $(JAR_FILE)
	@echo "Clean complete"

# Clean everything including dist
.PHONY: distclean
distclean: clean
	@echo "=== Removing all generated files ==="
	rm -rf $(DIST_DIR)
	@echo "Distclean complete"

# Help
.PHONY: help
help:
	@echo "Sailing Simulator Makefile"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  all        - Build JAR file (default)"
	@echo "  jar        - Build JAR file"
	@echo "  app        - Build macOS .app bundle (unsigned)"
	@echo "  sign       - Sign and notarize for distribution"
	@echo "  run        - Build and run the JAR"
	@echo "  run-app    - Build and run the .app bundle"
	@echo "  clean      - Remove build artifacts"
	@echo "  distclean  - Remove all generated files including dist"
	@echo "  help       - Show this help message"
