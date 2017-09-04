package org.apifusion.git_restful;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code DocToolDefinition} is the entry in docgenerator/doctools.csv which defines the order of tools for api docs generation.
 * The order is important as the first generated doc file will prevent the use of following tools.
 * I.e. if <b>.mvn</b> file is present and defined before <b>java</b> in the list, the <b>mvn javadoc:javadoc</b>
 * generated docs are sufficient and there is no need to generate docs from java files directly by javadoc CLI
 */
    public class
DocToolDefinition
{
    public String DocDirExt;    // the suffix appended to project name for docs folder
                                // like "javadoc" for "git-restful.javadoc"
    public String SrcExt;       // if the source file with given extension presented in folder the DocScript would run
                                // i.e. for "mvn' if source directory has *.mvn file run the docgenerator/mvn.sh (or bat)
    public String DocScript;    // relative to webapp/docgenerator folder path to doc generator script
                                // i.e. "java" refers to webapp/docgenerator/java.sh or webapp/docgenerator/java.bat

    public static DocToolDefinition[] ReadDocTools()
    {
        URL u = Thread.currentThread().getContextClassLoader().getResource( Application.DOCTOOLS_CSV );
        List<DocToolDefinition> ret= new ArrayList<DocToolDefinition>(4);
        try
        {   List<String> lines = Files.readAllLines( Paths.get( u.toURI()  ));
            for( String s : lines )
            {
                String[] a= s.split( "," );
                DocToolDefinition d = new DocToolDefinition();
                d.DocDirExt = a[0].trim();
                d.SrcExt    = a[1].trim();
                d.DocScript = a[2].trim();
                ret.add( d );
            }
        }catch( Exception e )
            {   e.printStackTrace(); }

        return ret.toArray( new DocToolDefinition[0] );
    }

    public boolean isServing( String[] files )
    {
        for( String n : files )
            if( n.endsWith( '.' + SrcExt ) )
                return true;
        return false;
    }
}
