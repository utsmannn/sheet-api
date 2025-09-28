#!/bin/bash

# Build and deploy frontend script for Sheet API
echo "🚀 Building React frontend..."

# Navigate to landingpage directory
cd landingpage || { echo "❌ landingpage directory not found"; exit 1; }

# Create .env file for Vite from environment variables
echo "✨ Creating .env file for Vite..."
printenv | grep -E "^(API_SECRET_KEY)" | sed -e 's/^/VITE_/' > .env

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Build the React app
echo "🔨 Building React app..."
npm run build

# Create static resources directory if it doesn't exist
mkdir -p ../src/main/resources/static

# Remove old static files
echo "🧹 Cleaning old static files..."
rm -rf ../src/main/resources/static/*

# Copy new build files
echo "📁 Copying new build files..."
cp -r dist/* ../src/main/resources/static/

echo "✅ Frontend build completed successfully!"
echo "📍 Static files deployed to: src/main/resources/static/"
echo "🌐 Landing page will be available at: http://localhost:8910/"