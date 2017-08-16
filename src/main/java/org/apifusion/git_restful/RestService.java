package org.apifusion.git_restful;
// http://localhost/af/ApiFusion.org-folders/ui/tools/importGit.html

import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Restful API controller
 */
        @RestController
    public class
RestService
{
        private static final Path
    TempPath = Application.TempPath;
        private static final String[]
    STRING_ARRAY = new String[]{};

        private static final Map<String, String>
    ext2suffix = new HashMap<>();
                static  {
                            ext2suffix.put("java"   ,"javadoc"  );
                            ext2suffix.put("js"     ,"jsdoc"    );
                            ext2suffix.put("py"     ,"sphynx"    );
                        }

        @CrossOrigin
        @RequestMapping("/git-restful")
            public
        Properties
    version() throws IOException, InterruptedException
    {
        java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("app.properties");
        java.util.Properties p = new Properties();
        p.load(is);
        is.close();
        return p;
    }
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
        @RequestMapping("/lock-branch/{repo}/{branch}")
            public
        String[]
    lockBranch( @PathVariable(value="repo") String repoName, @PathVariable(value="branch") String branch  ) throws IOException, InterruptedException
    {
        String[] r0 = exec( "git pull origin " + branch, repoName );
        String[] r1 = exec( "git checkout " + branch, repoName );
        String[] r = Stream.of(r0, r1).flatMap(Stream::of).toArray(String[]::new);
        // ["Your branch is behind 'origin/master' by 8 commits, and can be fast-forwarded.","  (use \"git pull\" to update your local branch)"]
        return r;
    }
//        @CrossOrigin
//        @RequestMapping("/list")
//            public
//        FolderEntry[]
//    listRepos() throws IOException, InterruptedException
//    {
//        return Files.list( TempPath ).map( FolderEntry::new ).toArray( FolderEntry[]::new );
//    }

        @CrossOrigin
        @RequestMapping("/list")
            public
        FolderEntry[]
    list( @RequestParam(value="repo"  ,required = false,defaultValue = "") String repo
        , @RequestParam(value="folder",required = false,defaultValue = "") String folder ) throws IOException, InterruptedException
    {
        if( 0==repo.length() )
            return Files.list( TempPath ).map( FolderEntry::new ).toArray( FolderEntry[]::new );

        Path root = TempPath.resolve( repo );
        Path repoPath = root.resolve( "."+File.separator+folder );
        if( repoPath.toFile().getCanonicalPath().startsWith( root.toFile().getCanonicalPath() ) )
            return Files.list( repoPath ).filter( n -> !n.toFile().getName().equals(".git") ).map( FolderEntry::new ).toArray( FolderEntry[]::new );
        throw new IOException( "malicious folder " + folder );
    }

        @CrossOrigin
        @RequestMapping("/docs") // http://localhost:8080/docs?repo=ApiFusion.org-folders&folder=
            public
        FolderEntry[]
    docs( @RequestParam(value="repo"  ,required = false ) String repo
        , @RequestParam(value="folder",required = false,defaultValue = "") String folder ) throws IOException, InterruptedException
    {
        /*
        *   1. Check whether docs exist
        *   1a. if exist check whether docs created after pulling the branch
        *       2a. if yes, include in return list
        *       2b. otherwise remove docs
        *   3. if docs not in result list
        *   4a. generate docs
        *   4b. add generated into result list
        * */

        Path root = TempPath.resolve( repo );
        long repoTime = root.toFile().lastModified();
        Path repoPath = root.resolve( "."+File.separator+folder );
        File repoPathFile = repoPath.toFile();
        String folderName = repoPathFile.getName();

        log( repoPath.toFile().getAbsolutePath() );
        if( !repoPath.toFile().getCanonicalPath().startsWith( root.toFile().getCanonicalPath() ) )
            throw new IOException( "malicious folder " + folder );
        Map<String, File> docRoots = Files.list( TempPath )
                    .map( f-> f.toFile() )
                    .filter( f ->
                    {   if( !f.getName().startsWith( repo+".") )
                            return false;
                        if( f.lastModified() >= repoTime )
                            return true;

                        deleteFolder( f.toPath(), "outdated" ); // 2b
                        return false;
                    })
                    .collect( Collectors.toMap( RestService::getExtension, Function.identity()) );

        FolderEntry[] ret = Files.list(repoPath).map( i -> i.toFile().getName() )
                            .map( RestService::getExtension ) // extension
                            .distinct()
                .map( ext->
                {   File d = docRoots.get( ext );
                    if( null != d )
                    {   Path p = d.toPath().resolve( folder );
                        return p;
                    }
                    // todo run doc script
                    return null;
                })
                .filter( p -> p != null )
                .map( FolderEntry::new ).toArray( FolderEntry[]::new );

        return ret;
//        return Files.list( TempPath ).filter( n -> n.toFile().getName().startsWith( repo+".") )
//                .map( FolderEntry::new ).toArray( FolderEntry[]::new );
    }

        private static String
    getExtension( File f ){    return getExtension( f.getName() );    }

        private static String
    getExtension( String n ){    return n.substring(n.lastIndexOf('.')+1);    }

        private void
    deleteFolder( Path rootPath, String reason )
    {   log( "removing "+reason, rootPath.toString() );
        try
        {   Files.walk(rootPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
//                .peek(System.out::println)
                .forEach(File::delete);
        }catch( IOException e )
            {   e.printStackTrace(); }
    }
        private void
    log( String s ){ System.out.println(s); }
        private void
    log( String s1, String s2 ){ System.out.print(s1); System.out.print(' '); System.out.println(s2); }
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
