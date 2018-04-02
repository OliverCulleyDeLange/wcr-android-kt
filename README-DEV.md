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


Useful Links:
Spock testing Kotlin Android
https://dzone.com/articles/testing-kotlin-with-spock-part-1-object
https://blog.andresteingress.com/2014/07/22/spock-junit-rules
https://stackoverflow.com/questions/48391716/spock-with-mockito-testing-kotlin-classes
https://proandroiddev.com/improve-your-tests-with-kotlin-in-android-pt-1-6d0b04017e80
https://github.com/dpreussler/kotlin-testrunner