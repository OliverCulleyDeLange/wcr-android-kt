chrome://inspect/#devices
Stetho the app to view DB contents

#TODO
- Get stuff from firestore after local db
    - Background sync? or on request
- Fix crash when expand bottom sheet when no topos
- Test DBs max length for strings - long paths could cause issues.
- Store user id of uploader to allow future editing
- Allow uploader to edit uploads?

Nice to haves:
- https://developer.android.com/kotlin/ktx#core-packages

- Clean architecture? RxJava? Single responsibility?


### Cloud storage decision
Wants:
- Low maintainance, low cost, quick turnaround, easy implementation
- Easy Auth / Login
- Integration with room / offline storage


Options:
- [Google Firebase](https://firebase.google.com/docs/firestore/quickstart?authuser=0)
    - *No searchability built in*
    - *Smaller feature set compared to parse*
    - Drop in Auth UI that just works.
    - NoSQL db doesn't play nice with Room our of the box.
    - Offline mode built in - stores locally
    - Probable data structure: (Assuming names are keys) (Complete redesign from room)
    ```
    Locations:
        Crag(Name, LatLng, List<Sector>)
            Sector(Name, LatLng, List<Topo>)
    Topos:
        Topo(Name, Image, List<Route>)
            Route(Name, Grade, Type, Description, Path)
    ```
- [Parse server](http://docs.parseplatform.org/android/guide/)
    - *Setup of server required - Heroku / ElasticBeanstalk + MongoDB hosted*

- [AWS Mobile Hub](https://docs.aws.amazon.com/aws-mobile/latest/developerguide/getting-started.html)
    - Drop in auth UI
    - Relatively easy to implement, but don't like their api or docs/guides
    - No offline mode but works well with room data objects

Scenarios to consider:
- Multiple users submitting stuff at the same time, IDs, overwrites etc

Resources:
https://proandroiddev.com/offline-apps-its-easier-than-you-think-9ff97701a73f

### GitLab
`brew install gitlab-runner`

To have launchd start gitlab-runner now and restart at login: <br />
`brew services start gitlab-runner`

Or, if you don't want/need a background service you can just run:<br />
`gitlab-runner start`

Changing a git remote: (https://help.github.com/articles/changing-a-remote-s-url/)<br />
`git remote set-url origin {url of new repo}`

Adding a git remote: (https://help.github.com/articles/adding-a-remote/)<br />
`git remote add origin {url of new repo}`

Verify a git remote:<br />
`git remote -v`


##### Data Structure:
```
- Crags (Location)
    - Sectors (Location)
        - Topos (Topo)
            - Routes (Route)
```

Useful Links:
Spock testing Kotlin Android
https://dzone.com/articles/testing-kotlin-with-spock-part-1-object
https://blog.andresteingress.com/2014/07/22/spock-junit-rules
https://stackoverflow.com/questions/48391716/spock-with-mockito-testing-kotlin-classes
https://proandroiddev.com/improve-your-tests-with-kotlin-in-android-pt-1-6d0b04017e80
https://github.com/dpreussler/kotlin-testrunner