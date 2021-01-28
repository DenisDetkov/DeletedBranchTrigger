@ECHO OFF

CMD /c "mvn compile"
CMD /c "mvn exec:java -Dexec.mainClass=Main"
