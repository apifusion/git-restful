package org.apifusion.git_restful;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class StaticResourceConfiguration extends WebMvcConfigurerAdapter
{
    // web access for src/main/resources/static files like http://localhost:8080/docgenerator/README.md

    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/",
            "classpath:/static/", "classpath:/public/" };

    @Override
    public void addResourceHandlers( ResourceHandlerRegistry registry )
    {
        registry.addResourceHandler( "/**" ).addResourceLocations( CLASSPATH_RESOURCE_LOCATIONS );
    }

    @Override
    public void configurePathMatch( PathMatchConfigurer configurer )
    {
        super.configurePathMatch( configurer );

        configurer.setUseSuffixPatternMatch( false );
    }
}