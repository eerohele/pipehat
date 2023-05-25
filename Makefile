.PHONY: test deploy

test:
	@clojure -X:dev cognitect.test-runner.api/test

pom: pom.xml
	@clojure -Spom

deploy: test, pom
	@mvn deploy
