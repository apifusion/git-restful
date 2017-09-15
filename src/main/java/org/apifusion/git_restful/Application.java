package org.apifusion.git_restful;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SpringBootApplication
    public class
Application
{
        protected static final Path
    TempPath = FileSystems.getDefault().getPath( System.getProperty("java.io.tmpdir"), "git-restful" );
        public static final Path
    DoctoolsPath = TempPath.resolve( "docgenerator" );

        public static final String
    DOC_RESOURCES = "static/docgenerator";
        public final static String
    DOCTOOLS_CSV = DOC_RESOURCES +"/doctools.csv";

		@Bean
		WebMvcConfigurer
	configurer ()
	{
	    // copy resources into tempPath
        try
        {   ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URI uri = cl.getResource( DOC_RESOURCES ).toURI();
            if( !copyFromJar( cl, DOC_RESOURCES, DoctoolsPath ) )
                copyFolder( Paths.get( uri ), DoctoolsPath );
        }catch( Exception e )
            {   e.printStackTrace(); }

        // enable Tomcat serving files from projects folder under  /pages
        // not called directly, rather server side redirected from /projects for a file
        return new WebMvcConfigurerAdapter()
		{
            @Override
            public void addResourceHandlers (ResourceHandlerRegistry registry)
			{	String pagesPath = TempPath.toFile().getPath().replaceAll("\\\\","/");
				System.out.println( "http://localhost:8080"+RestService.PAGES+" served from "+pagesPath );
				registry.addResourceHandler( RestService.PAGES+"**")
						.addResourceLocations("file:/"+pagesPath+'/' );
            }
        };
    }

    boolean copyFromJar( ClassLoader clazz, String path, Path dest ) throws URISyntaxException, IOException
    {
        URL dirURL = clazz.getResource( path );
        if( dirURL != null && dirURL.getProtocol().equals( "file" ) )
            return false;

        if( dirURL == null )  // In case of a jar file, we can't actually find a directory.* Have to assume the same jar as clazz.
            dirURL = clazz.getResource( path );

        if( dest.toFile().mkdirs() )
            System.out.println( "created " + dest );
        else
            System.out.println( "failed to create " + dest );


        if( !dirURL.getProtocol().equals( "jar" ) )
            return false;

        String jarPath = dirURL.getPath().substring( 5, dirURL.getPath()
                .indexOf( "!" ) ); //strip out only the JAR file
        System.out.println( "extracting|" + jarPath );

        JarFile jar = new JarFile( URLDecoder.decode( jarPath, "UTF-8" ) );
        Enumeration< JarEntry > entries = jar.entries(); //gives ALL entries in jar
        while( entries.hasMoreElements() )
        {
            JarEntry je = entries.nextElement();
            String name = je.getName()
            ,     entry = name.substring( name.lastIndexOf( '/' )+1 );
            if( !name.contains( path ) || entry.isEmpty() )
                continue;

            Path d = dest.resolve( entry );
            try
            {   Files.copy( jar.getInputStream( je ), d );
                System.out.println( "extracted |" + d + "| from |"+name );
            }catch( java.nio.file.FileAlreadyExistsException ex )
                {   System.out.println( "skipping existing |" + d ); }
            catch( Exception ex )
                {   System.out.println( "creation ERROR of |" + d + "| "+ex.getMessage()); }

        }
        return true;
    }
        public  static void
    copyFolder( Path src, Path dest )
    {
        try
        {   Files.walk( src )
            .forEach( s ->
            {   Path d = dest.resolve( src.relativize(s) );
                try
                {   if( Files.isDirectory( s ) )
                    {   if( !Files.exists( d ) )
                            Files.createDirectory( d );
                        return;
                    }
                    if( Files.exists( d ) )
                        System.out.println( "skipping existing |" + d );
                    else
                    {   Files.copy( s, d );
                        System.out.println( "copied |" + d + "| from |"+s );
                    }
                }catch( Exception ex )
                    { System.out.println( "creation ERROR of |" + d + "| "+ex.getMessage());  }
            });
        }catch( Exception ex )
            {   ex.printStackTrace(); }
    }

	    public static void
    main(String[] args)
        {	SpringApplication.run(Application.class, args);	}
}
