# git-restful docgenerator
Folder comprise 
* source type map in doctools.csv
* Documentation rendering scripts 

## Use by git-restful
During the first **git-restful.jar** load the resource src/main/resources/static/docgenerator folder is extracted into work 
directory ( like /tmp/git-restful/docgenerator in Linux ). 

You could modify scripts and CSV according to project environment. 
The files would **not** be overridden on following restarts.

## Source type map in doctools.csv
The **doctools.csv** holds fields for
* DocDirExt - documentation directory suffix. Used for location documentation as sibling to project source 
    and as sub-folder in source directory
* SrcExt - source file suffix( extension or file name ). 
* DocScript - If the matching to SrcExt file found in folder, run the the script from the CSV record


The rules order matters as listed first will be executed first. 
Some rules could generate same documentation type. In such case the earlier generated docs will prevent the generation 
of following.
 
For example, javadoc generated from maven record will create ${project}.javadoc folder, following java files in sources 
sub-folder have a record for javadoc generation. But since the docs for all sources already exist the record with 
**.java** will not be executed.     
 
## Documentation rendering scripts


All scripts implementing the same CLI. Script is picked according DocScript column in doctools.csv and run from source 
folder.  

The script will ba called if source folder has at least one file with extension defined in CSV.

Each script has two implementations for windows BAT and Unix bash. 

* $0 **name**  matches the file extension just for convenience, script is selected from CVS. 
* $1 **target** folder path 

#ToDo
* Linux version of java and mvn scripts
* -Dgit-restful.work.path defaults to '.' ( current directory ) and if missed to /tmp/git-restful  