package org.apifusion.git_restful;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        {   URL u = Thread.currentThread().getContextClassLoader().getResource( DOC_RESOURCES );
            copyFolder( Paths.get( u.toURI()  ), DoctoolsPath );
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
        public  static void
    copyFolder( Path src, Path dest )
    {
        try
        {   Files.walk( src )
            .forEach( s ->
            {   try
                {   Path d = dest.resolve( src.relativize(s) );
                    if( Files.isDirectory( s ) )
                    {   if( !Files.exists( d ) )
                            Files.createDirectory( d );
                        return;
                    }
                    if( Files.exists( d ) )
                        System.out.println( "using existing " + d );
                    else
                        Files.copy( s, d );
                }catch( Exception e )
                    { e.printStackTrace(); }
            });
        }catch( Exception ex )
            {   ex.printStackTrace(); }
    }

	    public static void
    main(String[] args)
        {	SpringApplication.run(Application.class, args);	}
}
