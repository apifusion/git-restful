echo javadoc -d %1 * -sourcepath .
mkdir %1
javadoc -d %1 * -sourcepath .
echo done %1