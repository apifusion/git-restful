set DOC_TMP=%1\javadoc
echo javadoc -d %DOC_TMP% * -sourcepath .
mkdir %1
javadoc -d %DOC_TMP% * -sourcepath .

echo done %DOC_TMP%