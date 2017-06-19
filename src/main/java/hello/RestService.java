package hello;

import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Restful API controller
 */
        @RestController
    public class
RestService
{
    static final String BASH_sig = "#!/usr/bin/env bash ";
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
    {
        return exec( "git branch -r", repoName);
    }

        String[]
    exec( String cmd, String repo ) throws IOException, InterruptedException
    {   Path repoP = TempPath.resolve( repo );
        File repoPath = repoP.toFile();
        Files.createDirectories( repoP );
        System.out.println(repoPath);

        File       cmdFile = File.createTempFile( "cmd", ".bat", TempPath.toFile() ); cmdFile.deleteOnExit();
        boolean      isWin = File.separatorChar == '\\';
        String[] batchBody = { isWin ? "@echo off" : BASH_sig, cmd };
        String      exeCmd = ( isWin ? "cmd /c " : "/usr/bin/env bash " ) + cmdFile.getPath();
        System.out.println(exeCmd);
        Files.write( cmdFile.toPath(), Arrays.asList(batchBody) , StandardCharsets.UTF_8 );
        Process p = Runtime.getRuntime().exec( exeCmd , null, repoPath );

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String s;
        ArrayList<String> ret=new ArrayList<>();
        while( (s = stdInput.readLine()) != null )
            ret.add(s);

        while( (s = stdError.readLine()) != null)
            System.err.println(s);

        p.waitFor();
        System.out.println(cmdFile+" done");
        return ret.toArray( new String[]{} );
    }
}
