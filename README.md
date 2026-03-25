# Copas

This project includes CPU mode and local multiplayer over sockets.

## Generate Windows x64 EXE

A workflow is included at `.github/workflows/windows-exe.yml`.

### 1) Push changes to GitHub

```zsh
git add .
git commit -m "Add Windows EXE build workflow"
git push origin main
```

### 2) Run workflow

1. Open your repo on GitHub.
2. Go to **Actions**.
3. Select **Build Windows EXE**.
4. Click **Run workflow**.

### 3) Download installer

After the workflow succeeds:

1. Open that workflow run.
2. Download artifact **Copas-Windows-EXE**.
3. Use the `.exe` installer on Windows x64 machines.

## Run local smoke test

```zsh
cd "/Users/yassinebellali/Desktop/EHEI/java/copas"
rm -rf out
mkdir -p out
javac -d out $(find src -name "*.java")
java -Djava.awt.headless=true -cp "out:res" main.LocalMultiplayerSmokeTest
```

