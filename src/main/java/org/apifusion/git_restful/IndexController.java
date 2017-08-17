package org.apifusion.git_restful;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class IndexController implements ErrorController
{

    private static final String PATH = "/error";
    private static final String PAGES = "/pages/";

            @RequestMapping(value = PATH) public
        FolderEntry[]
    error( HttpServletRequest request ) throws IOException
    {
        String originalUri = ( String ) request.getAttribute( RequestDispatcher.FORWARD_REQUEST_URI);
        Path p = Application.TempPath;
        if( originalUri.startsWith( PAGES ) )
            p = p.resolve( originalUri.substring( PAGES.length() ) );

        System.out.println( p );
        return Files.list( p ).map( FolderEntry::new ).toArray( FolderEntry[]::new );
    }

    @Override
    public String getErrorPath()
        {   return PATH;  }
}