# Build

```
$ mvn clean package
```

# Run

```
$ java -jar target/cbtail-*-jar-with-dependencies.jar
```

# Release

```
$ git tag VERSION
$ # Update version to RELEASE
$ mvn clean package
$ cp target/cbtail-*-jar-with-dependencies.jar cbtail.jar  
$ # update version to NEXT-SNAPSHOT
$ git add -A
$ git commit -m 'Release: VERSION'
$ git push --tags
# Update cbtail.rb in homebrew-tap
```
