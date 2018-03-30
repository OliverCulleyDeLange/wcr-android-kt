GitLab:
brew install gitlab-runner

To have launchd start gitlab-runner now and restart at login:
  $ brew services start gitlab-runner
Or, if you don't want/need a background service you can just run:
  $ gitlab-runner start

Changing a git remote: (https://help.github.com/articles/changing-a-remote-s-url/)
  $ git remote set-url origin {url of new repo}
Adding a git remote: (https://help.github.com/articles/adding-a-remote/)
  $ git remote add origin {url of new repo}
Verify a git remote:
  $ git remote -v


Data Structure:

- Crags
    - Sectors
        - Topos
            - Climbs