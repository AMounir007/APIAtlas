param(
    [string]$RemoteUrl = "https://github.com/AMounir007/APIAtlas.git",
    [string]$Branch = "main",
    [string]$CommitMessage = "feat: publish API Atlas project"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

Write-Host "API Atlas GitHub push helper" -ForegroundColor Cyan
Write-Host "Project: $ProjectRoot"
Write-Host "Remote : $RemoteUrl"
Write-Host "Branch : $Branch"

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "Git is not installed or not available in PATH. Install Git for Windows, then rerun this script."
}

if (-not (Test-Path ".git")) {
    Write-Host "Initializing Git repository..." -ForegroundColor Yellow
    git init
}

Write-Host "Staging project files..." -ForegroundColor Yellow
git add .

$hasStagedChanges = $false
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    $hasStagedChanges = $true
}

if ($hasStagedChanges) {
    Write-Host "Creating commit..." -ForegroundColor Yellow
    git commit -m $CommitMessage
} else {
    Write-Host "No staged changes to commit." -ForegroundColor Green
}

Write-Host "Setting branch to $Branch..." -ForegroundColor Yellow
git branch -M $Branch

$existingOrigin = git remote get-url origin 2>$null
if ($LASTEXITCODE -eq 0) {
    if ($existingOrigin -ne $RemoteUrl) {
        Write-Host "Updating origin remote from $existingOrigin to $RemoteUrl..." -ForegroundColor Yellow
        git remote set-url origin $RemoteUrl
    } else {
        Write-Host "Origin remote already configured." -ForegroundColor Green
    }
} else {
    Write-Host "Adding origin remote..." -ForegroundColor Yellow
    git remote add origin $RemoteUrl
}

Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
try {
    git push -u origin $Branch
} catch {
    Write-Host "Initial push failed. If the remote repository already contains files, pulling with unrelated histories may be required." -ForegroundColor Red
    Write-Host "Run this if you trust merging the remote contents:" -ForegroundColor Yellow
    Write-Host "git pull origin $Branch --allow-unrelated-histories" -ForegroundColor White
    Write-Host "git push -u origin $Branch" -ForegroundColor White
    throw
}

Write-Host "Push completed successfully." -ForegroundColor Green
