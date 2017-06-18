package hello;

import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;

/**
 * Restful API controller
 */
        @RestController
    public class
RestService
{
    static final String BASH_sig = "#!/usr/bin/env bash";
    static final Path   TempPath = FileSystems.getDefault().getPath( System.getProperty("java.io.tmpdir"), "git-restful" );


       @RequestMapping("/lock-repo")
            public
        String
    lockRepo( @RequestParam(value="url") String url ) throws IOException, InterruptedException
    {

        exec( "git clone " + url, ".");

        return url.substring( url.lastIndexOf('/')+1, url.lastIndexOf('.') );
    }

       @RequestMapping("/branches")
            public
        String[]
    branches( @RequestParam(value="repo") String repoName ) throws IOException, InterruptedException
    {   exec( "git branch -r", repoName);
        return new String[]{"a","b"};
    }

        void
    exec( String cmd, String repo ) throws IOException, InterruptedException
    {   File repoPath = TempPath.resolve(repo).toFile();

        File cmdFile = File.createTempFile( "cmd", ".bat", TempPath.toFile() );
        cmdFile.deleteOnExit();
        String[] batchBody = { BASH_sig, cmd };
        Files.write( cmdFile.toPath(), Arrays.asList(batchBody) , StandardCharsets.UTF_8 );
        String exeCmd = ( File.separatorChar == '\\'? "cmd /c " : "" ) + cmdFile.getPath();
        Process p = Runtime.getRuntime().exec( exeCmd , null, repoPath );
        p.waitFor();
    }
}
