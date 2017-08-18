package org.apifusion.git_restful;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.nio.file.FileSystems;
import java.nio.file.Path;

    @SpringBootApplication
    public class
Application
{
        protected static final Path
    TempPath = FileSystems.getDefault().getPath( System.getProperty("java.io.tmpdir"), "git-restful" );

		@Bean
		WebMvcConfigurer
	configurer () // enable Tomcat serving files from projects folder under  /pages
	{             // not called directly, rather server side redirected from /projects for a file
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
	public static void main(String[] args)
        {	SpringApplication.run(Application.class, args);	}
}
