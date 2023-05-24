.PHONY: test deploy

test:
	@clj -X:dev cognitect.test-runner.api/test

pom: pom.xml
	@clj -Spom

deploy: test, pom
	@mvn deploy
