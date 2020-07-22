FROM java:8
WORKDIR /
ADD build/libs/example.jar example.jar
EXPOSE 8080
CMD java - jar example.jar