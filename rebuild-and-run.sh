#!/bin/bash

# Script to clean, rebuild cache and game-server, then start the game server
# This is a complete rebuild and run script for development

set -e  # Exit on error

echo "🧹 Cleaning builds..."
echo ""

# Clean cache build
echo "📦 Cleaning cache module..."
./gradlew :cache:clean --quiet

# Clean game-server build
echo "🎮 Cleaning game-server module..."
./gradlew :game-server:clean --quiet

echo ""
echo "🔨 Rebuilding everything..."
echo ""

# Build the cache module first to ensure latest code is compiled
echo "📦 Building cache module..."
./gradlew :cache:build --quiet

# Run the cache build task
echo "🗄️  Building cache database files..."
./gradlew :cache:buildCache

# Build the game-server
echo "🎮 Building game-server..."
./gradlew :game-server:build --quiet

echo ""
echo "✅ Build complete!"
echo ""
echo "🚀 Starting game server..."
echo ""

# Start the game server
./gradlew :game-server:run

