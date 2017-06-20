package hello;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

    public class
FolderEntry
{
	public FolderEntry( Path p )
	{	File f = p.toFile();
		            name = f.getName();
		          length = f.length();
		          isFile = f.isFile();
		     isDirectory = f.isDirectory();
		          isLink = Files.isSymbolicLink(p);
        try{  linkTarget = isLink ? Files.readSymbolicLink(p).toString() : "";  }
        catch( IOException e ){  e.printStackTrace(); }
    }
	public String 	name;
	public long		length;
	public boolean 	isDirectory;
	public boolean 	isFile;
	public boolean 	isLink;
	public String   linkTarget;
}
