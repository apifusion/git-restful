package org.apifusion.git_restful;
// http://localhost/af/ApiFusion.org-folders/ui/tools/importGit.html

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
        private static final String[]
    INDEX_FILES = new String[]{"index.html","package-summary.html"};

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

    protected static final String PROJECTS = "/projects/";
    protected static final String PAGES = "/pages/";
    protected static final String DOCS = "/docs/";

        @CrossOrigin
        @RequestMapping( PROJECTS + "**" )
            public
        FolderEntry[]
    folderList(  HttpServletRequest request,  HttpServletResponse response )
        throws IOException, InterruptedException, ServletException
    {   String folder =(String) request.getAttribute( HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE );
        String p = folder.substring( PROJECTS.length() );
        Path pp = TempPath.resolve( p );
        File f = pp.toFile();
        if( !p.endsWith( "/" ) )
            return new FolderEntry[]{ new FolderEntry( pp ) };

        if( f.isFile() )
        {
            request.getRequestDispatcher(  PAGES + p  ).forward( request, response );
            return null;
        }
        if( ".git".equals( f.getName()) )
            return new FolderEntry[]{};
        return Files.list( pp ).map( FolderEntry::new ).toArray( FolderEntry[]::new );
    }

//        @CrossOrigin
//        @RequestMapping("/list")
//            public
//        FolderEntry[]
//    list( @RequestParam(value="repo"  ,required = false,defaultValue = "") String repo
//        , @RequestParam(value="folder",required = false,defaultValue = "") String folder ) throws IOException, InterruptedException
//    {
//        if( 0==repo.length() )
//            return Files.list( TempPath ).map( FolderEntry::new ).toArray( FolderEntry[]::new );
//
//        Path root = TempPath.resolve( repo );
//        Path repoPath = root.resolve( "."+File.separator+folder );
//        if( repoPath.toFile().getCanonicalPath().startsWith( root.toFile().getCanonicalPath() ) )
//            return Files.list( repoPath ).filter( n -> !n.toFile().getName().equals(".git") ).map( FolderEntry::new ).toArray( FolderEntry[]::new );
//        throw new IOException( "malicious folder " + folder );
//    }

        @CrossOrigin
        @RequestMapping( DOCS + "{repo}/**" ) // http://localhost:8080/docs/git-restful/src/main/java/org/apifusion/git_restful/Application.java
            public
        FolderEntry[]
    docs( @PathVariable(value="repo") String repoName, HttpServletRequest request ) throws IOException, InterruptedException
    {
        /*
        *   1. check for outdated docs in ${repo}.javadoc folder, remove older than sources
        *   3. if docs not in result list
        *   4a. generate docs
        *   4b. add generated into result list
        * */

        String urlPath =(String) request.getAttribute( HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE );
        String srcPath = urlPath.substring( DOCS.length() );
        String folder  = srcPath.substring( repoName.length()+1 );

        Path repoRoot = TempPath.resolve( repoName );
        long repoTime = repoRoot.toFile().lastModified();
        Path repoPath = TempPath.resolve( srcPath );
        File repoPathFile = repoPath.toFile();
        String folderName = repoPathFile.getName();

        log( repoPathFile.getAbsolutePath() );
        if( !repoPathFile.getCanonicalPath().startsWith( repoRoot.toFile().getCanonicalPath() ) )
            throw new IOException( "malicious folder " + srcPath );

        // 1. check for outdated docs in ${repo}.javadoc folder, remove older than sources
//todo move docroots into directory section & use directory instead of root
        final DocToolDefinition[] docTools =  DocToolDefinition.ReadDocTools();

        if( repoPathFile.isDirectory() )
        {
            Map<String, File> docPaths = Files.list( TempPath )
                    .map( Path::toFile )
                    .filter( f ->
                    {   if( !f.getName().startsWith( repoName+".") )
                            return false;
                        if( f.lastModified() >= repoTime )
                            return true;

                        deleteFolder( f.toPath(), "outdated" ); // delete outdated docs root folder
                        return false;
                    }).map( f->f.toPath().resolve( folder ).toFile() )
                    .filter( f ->
                    {   if( !f.exists() )
                            return false;
                        if( f.lastModified() >= repoTime )
                            return true;

                        deleteFolder( f.toPath(), "outdated" ); // delete outdated docs+path folder
                        return false;
                    }).collect( Collectors.toMap( RestService::getDocExt, Function.identity()) );
// todo validate getDocExt

//            ArrayList<FolderEntry> ret = new ArrayList<>();
            String[]    srcFiles = TempPath.resolve( srcPath ).toFile().list();

            //  for each tool with file type in directory
            //      if package doc not exist
            //          generate doc
            //      add package doc to result
            String[] out;

            for( DocToolDefinition docTool: docTools )
            {
                String docExt = docTool.DocDirExt;
                if( docPaths.containsKey( docExt ) ) // already generated
                    continue;
                if( docTool.isServing(srcFiles) )
                {   Path docRoot = TempPath.resolve( repoName+"."+docExt );
                    Path    docFolder = docRoot.resolve( folder );
                    docPaths.put( docExt, docFolder.toFile() );
                    String     script = docTool.getScriptPath();
                    out = exec( script+' '+docFolder , repoPathFile );
                }
            }

            // all {docsFolders}.*/{folder}[/index.html]
            FolderEntry[] ret =  docPathStream( repoName ).map( docRootPath ->
                {   Path dp = docRootPath.resolve( folder );
                    String docExt = getDocExt( dp.toFile() );
                    for( String indexName : INDEX_FILES )
                    {   File f = dp.resolve( indexName ).toFile();
                        if( f.exists() )
                            return new FolderEntry( f.toPath() );
                        f = dp.resolve( docExt ).resolve( indexName ).toFile();
                        if( f.exists() )
                            return new FolderEntry( f.toPath() );
                    }
                    return new FolderEntry( dp );
                })
                .toArray( FolderEntry[]::new );
            return ret;
        }
        // all {docsFolders}.*/{folder}/{noExt}.html
        return docPathStream( repoName ).map( docRootPath ->
                {   String docPath = folder.substring(  0, folder.length()-getExtension( repoPathFile ).length() )+"html"; // todo .* instead of HTML
                    Path dp = docRootPath.resolve( docPath );
                    if( Files.exists( dp ) )
                        return new FolderEntry( dp );
                    Path docFolder = dp.getParent();
                    String docFileName = dp.toFile().getName();

                    for( DocToolDefinition docTool: docTools )
                    {   Path p = findInPath( docFolder.resolve( docTool.DocDirExt), docFileName );
                        if( null != p )
                            return new FolderEntry( p );
                    }
                    return null;

//                    Path ret = Arrays.stream( docTools )
//                            .map( docTool -> findInPath( docFolder.resolve( docTool.DocDirExt), docFileName ) )
//                            .filter( Objects::nonNull )
//                            .findFirst()
//                            .get();
//
//                    return ret;
                }).filter( e-> null != e )
                .toArray( FolderEntry[]::new );
    }
        Path
    findInPath( Path searchFrom, String fileName )
    {   try
        {   if( Files.exists( searchFrom ) )
                return Files.walk( searchFrom )
                            .filter( p-> fileName.equals( p.toFile().getName() ) )
                            .findFirst().get();
        }catch( IOException e )
            {   e.printStackTrace(); }
        return null;
    }
        Stream<Path>
    docPathStream( String repoName ) throws IOException
        { return Files.list( TempPath ).filter( n -> n.toFile().getName().startsWith( repoName+".") ); }

        private Stream<Path>
    docTools() throws IOException
    {
        // todo extract tools from resources or list from FS
        return Files.list( TempPath.resolve( "doctools" ) );
    }
        private static String
    getExtension( File f ){    return getExtension( f.getName() );    }
        private static String
    getDocExt( File f )
    {   String r = TempPath.relativize( f.toPath() ).getName( 0 ).toString();
        int i = r.indexOf( '.' )+1;
        String ret = r.substring( i );
        return ret;
    }

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
    exec( String cmd, String repo ) throws IOException
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
        return exec( cmd, repoPath );
    }
        class
    StreamGobbler extends Thread
    {
        InputStream is;
        String type;
        OutputStream os;

        StreamGobbler(InputStream is, String type)
        {
            this(is, type, null);
        }
        StreamGobbler(InputStream is, String type, OutputStream redirect)
        {
            this.is = is;
            this.type = type;
            this.os = redirect;
        }

        public void run()
        {
            try
            {   PrintWriter pw = null;
                if (os != null)
                    pw = new PrintWriter( os);

                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line=null;
                while ( (line = br.readLine()) != null)
                {
                    if (pw != null)
                        pw.println(line);
                    System.out.println(type + ">" + line);
                }
                if (pw != null)
                    pw.flush();
            } catch (IOException ioe)
                {
                ioe.printStackTrace();
                }
        }
    }
            private
        String[]
    exec( String cmd, File execPath )
    {
        ArrayList<String> ret=new ArrayList<>();
        try
        {   File       cmdFile = File.createTempFile( "cmd", ".bat", TempPath.toFile() ); cmdFile.deleteOnExit();
            boolean      isWin = File.separatorChar == '\\';
            String[] batchBody = { isWin ? "@echo off" : "#!/usr/bin/env bash ", cmd };
            String      exeCmd = ( isWin ? "cmd /c "   :   "/usr/bin/env bash " ) + cmdFile.getPath();
            System.out.println(exeCmd);
            Files.write( cmdFile.toPath(), Arrays.asList(batchBody) , StandardCharsets.UTF_8 );
            Process p = Runtime.getRuntime().exec( exeCmd , null, execPath );
            StreamGobbler err = new StreamGobbler( p.getErrorStream(), "ERROR" );
            StreamGobbler out = new StreamGobbler( p.getInputStream(), "OUTPUT");

            err.start();
            out.start();

            int exitCode = p.waitFor();
            System.out.println( cmd + " | DONE "+cmdFile+" | exit code "+ exitCode  );
            cmdFile.delete();
        }catch( IOException ioe )
            {
                ioe.printStackTrace();
            }
        catch( InterruptedException ie )
            {
                System.err.println(ie.getMessage());
            }

        return ret.toArray( STRING_ARRAY );
    }
}
