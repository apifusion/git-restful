set DOC_TMP=%1\src\main\java
echo mvn javadoc:javadoc
call mvn javadoc:javadoc
mkdir %1
mkdir %DOC_TMP%
echo copy target/site/apidocs %DOC_TMP%
xcopy target\site\apidocs %DOC_TMP% /E /Y

echo done %1