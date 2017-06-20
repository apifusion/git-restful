package org.apifusion.git_restful;

import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Restful API controller
 */
        @RestController
    public class
RestService
{
        private static final Path
    TempPath = FileSystems.getDefault().getPath( System.getProperty("java.io.tmpdir"), "git-restful" );
        private static final String[]
    STRING_ARRAY = new String[]{};

        @CrossOrigin
        @RequestMapping("/lock-repo")
            public
        String
    lockRepo( @RequestParam(value="url") String url ) throws IOException, InterruptedException
    {
        String repo = url.substring( url.lastIndexOf('/')+1, url.lastIndexOf('.') );
        File repoPath = TempPath.resolve( repo ).toFile();
        if( repoPath.exists() )
        {
            String[] r = exec( "git config --get remote.origin.url", repo );
            if( r[0].contains( url ) )
            {   exec( "git fetch --all", repo );
                return repo;
            }
            repoPath.delete();
        }
        exec( "git clone " + url, ".");
        return repo;
    }
        @CrossOrigin
        @RequestMapping("/list/{repo}/branches")
            public
        String[]
    branches( @PathVariable(value="repo") String repoName ) throws IOException, InterruptedException
    {
        String[] r = exec( "git branch -r", repoName );
        for( int i =0 ;i<r.length; i++ )
            r[i] = r[i].substring( r[i].lastIndexOf("origin/")+7 );
        // todo include releases
        return r;
    }
        @CrossOrigin
        @RequestMapping("/lock-branch")
            public
        String[]
    lockBranch( @RequestParam(value="repo") String repoName, @RequestParam(value="branch") String branch  ) throws IOException, InterruptedException
    {
        String[] r = exec( "git checkout " + branch, repoName );

        return r;
    }
        @CrossOrigin
        @RequestMapping("/list")
            public
        FolderEntry[]
    listRepos() throws IOException, InterruptedException
    {
        return Files.list( TempPath ).map( FolderEntry::new ).toArray( FolderEntry[]::new );
    }

        @CrossOrigin
        @RequestMapping("/list/{repo}")
            public
        FolderEntry[]
    listRepoRoot( @PathVariable(value="repo") String repo ) throws IOException, InterruptedException
    {
       return list( repo, "." );
    }
        @CrossOrigin
        @RequestMapping("/list/{repo}/{folder}")
            public
        FolderEntry[]
    list( @PathVariable(value="repo") String repo, @PathVariable(value="folder") String folder ) throws IOException, InterruptedException
    {
        Path root = TempPath.resolve( repo );
        Path repoPath = root.resolve( "."+File.separator+folder );
        if( repoPath.toFile().getCanonicalPath().startsWith( root.toFile().getCanonicalPath() ) )
            return Files.list( repoPath ).filter( n -> !n.toFile().getName().equals(".git") ).map( FolderEntry::new ).toArray( FolderEntry[]::new );
        throw new IOException( "malicious folder " + folder );
    }
            private
        String[]
    exec( String cmd, String repo ) throws IOException, InterruptedException
    {
        System.out.println( cmd );

        Path repoP = TempPath.resolve( repo );
        File repoPath = repoP.toFile();
        if( !repoPath.exists() )
        {
            System.err.println( repo );
            throw new IOException( "malicious repo name " + repo );
        }

        Files.createDirectories( repoP );
        System.out.println(repoPath);

        File       cmdFile = File.createTempFile( "cmd", ".bat", TempPath.toFile() ); cmdFile.deleteOnExit();
        boolean      isWin = File.separatorChar == '\\';
        String[] batchBody = { isWin ? "@echo off" : "#!/usr/bin/env bash ", cmd };
        String      exeCmd = ( isWin ? "cmd /c "   :   "/usr/bin/env bash " ) + cmdFile.getPath();
        System.out.println(exeCmd);
        Files.write( cmdFile.toPath(), Arrays.asList(batchBody) , StandardCharsets.UTF_8 );
        Process p = Runtime.getRuntime().exec( exeCmd , null, repoPath );

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String s;
        ArrayList<String> ret=new ArrayList<>();
        while( (s = stdInput.readLine()) != null )
        {   ret.add( s );
            System.out.println( s );
        }
        while( (s = stdError.readLine()) != null)
            System.err.println(s);

        p.waitFor();
        System.out.println( cmdFile + " done" );
        cmdFile.delete();
        return ret.toArray( STRING_ARRAY );
    }
}
