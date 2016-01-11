/*
 * $Id$
 * 
 * Copyright (c) 2015, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.script;

import com.google.common.base.Charsets;
import java.io.*;
import java.util.*;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Wraps a groovy script engine to provide things like API
 *  setup, shared and local bindings, etc..  This class uses the groovy
 *  classes directly instead of the JSR223 scripting support built into Java
 *  because the built in support obscures the script names in stack traces.
 *
 *  @author    Paul Speed
 */
public class ScriptEnvironment {

    static Logger log = LoggerFactory.getLogger(ScriptEnvironment.class);
    
    private final String name;
    private final GroovyShell groovy;
    private final Binding context;
    private boolean initialized;

    private final List<Script> api = new ArrayList<>();

    public ScriptEnvironment( String name ) {
        this.name = name;
        this.context = new Binding();
        this.groovy = new GroovyShell(context);       
    }

    /**
     *  Adds resource references to scripts that should be compiled and
     *  executed as part of this environments API layer.
     */
    public ScriptEnvironment addApiResources( String... resources ) {
        for( String s : resources ) {
            api.add(compileApiResource(s));
        }
        return this;
    }
    
    /**
     *  Adds File references to scripts that should be compiled and
     *  executed as part of this environments API layer.
     */
    public ScriptEnvironment addAApiFiles( File... files ) {
        for( File f : files ) {
            api.add(compileApiFile(f));
        }
        return this;
    }

    /**
     *  Sets a variable that will be available globally to the scripts
     *  in this environment.
     */
    public void setBinding( String name, Object value ) {
        context.setProperty( name, value );
    }
    
    /**
     *  Returns a variable that is available globally to the scripts
     *  in this environment.
     */
    public Object getBinding( String name ) {
        return context.getProperty(name);
    }

    /**
     *  Initializes the API that the 'runtime' scripts will rely upon.
     *  IF this is not called by the application then this is called automatically 
     *  the first time that a script is evaluated.
     */
    public void initializeApi() {
        if( initialized ) {
            return;
        }
        
        for( Script script : api ) {
            if( log.isDebugEnabled() ) {
                log.debug("evaluating API script:" + script);
            }
                    
            //script.setBinding(localBindings(bindings, source));
            Object result = script.run(); 
            if( log.isDebugEnabled() )
                log.debug("result:" + result);
        }
    }

    protected Script compileApiResource( String resource ) {
        InputStream in = getClass().getResourceAsStream(resource);
        if( in == null ) {
            throw new RuntimeException("Script resource not found for:" + resource);
        }
        return compile(in, resource); 
    } 

    protected Script compileApiFile( File file ) {
        return compile(file); 
    } 

    protected Script compile( Reader in, String fileName )  {    
        // Make sure the name is ok to be a class name
        if( fileName.indexOf('-') >= 0 ) {
            fileName = fileName.replaceAll("-", "_");
        }            
        Script result = groovy.parse(in, fileName);
        return result;
    }
 
    protected Script compile( InputStream in, String fileName ) {
        return compile(new InputStreamReader(in, Charsets.UTF_8), fileName);
    }

    protected Script compile( File f )  {
        try {
            return compile(new FileReader(f), String.valueOf(f));
        } catch( IOException e ) {
            throw new RuntimeException("Error compiling:" + f, e);
        }
    }
           
    public Object evalResource( String s )
    {
        InputStream is = getClass().getResourceAsStream(s);
        if( is == null )
            throw new IllegalArgumentException( "Script resource not found for:" + s );
        return eval(is, s);
    }
    
    public Object eval( URL u ) {
        try {
            return eval(u.openStream(), String.valueOf(u));
        } catch( IOException e ) {
            throw new RuntimeException("Error reading:" + u, e);
        }
    }

    public Object eval( URI u ) {
        try {
            return eval(u.toURL());
        } catch( MalformedURLException e ) {
            throw new RuntimeException("Script URI error:" + u, e);
        }
    }
 
    public Object eval( File f ) {
        return eval(f.toURI());
    }
    
    public Object eval( InputStream is, String s ) {
        if( log.isDebugEnabled() ) {
            log.debug( "evaluating:" + s );
        }
        Script script = compile(is, s);

        int before = context.getVariables().size();
        Object result = script.run(); 
        if( log.isTraceEnabled() )
            log.trace("result:" + result);                
            
        if( before != context.getVariables().size() ) {
            log.warn("Binding count increased executing:" + s + "  keys:" + context.getVariables().keySet());        
        }
        
        return result;
    }
}


/*
package mythruna.script;

import com.google.common.collect.Sets;
import java.io.*;
import java.net.URL;
import java.util.*;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import com.google.common.io.CharStreams;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import mythruna.LifeLog;
import org.codehaus.groovy.runtime.MethodClosure;

import org.progeeks.util.log.Log;

public class ScriptEnvironment
{
    static Log log = Log.getLog();
    private LifeLog life;

    private static final String RESOURCE_PREFIX = "";
    private static final String FILE_PREFIX = "file:";
    private static boolean includeSourceInErrors = false;

    private List<Script> api = new ArrayList<Script>();
    private Map<Script, Object> sources = new HashMap<Script, Object>();
    private Map<Script, String> sourceCode = new HashMap<Script, String>();
    private GroovyShell engine;
    private GroovyShell compiler;
    private Binding bindings;

    public ScriptEnvironment( String name, Object... apiScripts )
    {
        this.life = LifeLog.get(log, name); 
        //ScriptEngineManager factory = new ScriptEngineManager();        
        //this.engine = factory.getEngineByName("groovy");
        //this.compiler = (Compilable)engine;
        //this.bindings = engine.createBindings();
        this.bindings = new Binding();
        this.engine = this.compiler = new GroovyShell(bindings);
        
        bindings.setProperty( "bindings", bindings.getVariables() );
 
        compileApi( apiScripts );
    }
    
    protected void compileApi( Object... apiScripts )
    {
        for( Object o : apiScripts )
            {
            //try
            //    {
                if( o instanceof String )
                    compileApiResource( (String)o );
                else if( o instanceof File )
                    compileApiFile( (File)o );
            //    }
            //catch( ScriptException e )
            //    {
            //    throw new RuntimeException( "Error compiling script:" + o, e );
            //    }
            }
    }

    public void addApiResource( String s )
    {
        life.log("addApiResource(" + s + ")");
        //try
        //    {
            compileApiResource(s);
        //    }
        //catch( ScriptException e )
        //    {
        //    throw new RuntimeException( "Error compiling script:" + s, e );
        //    }
    }    

    protected void addApiScript( Script script, String source )
    {
        log.info( "addApiScript(" + script + ", " + source + ")" );
        api.add(script);
        sources.put( script, source );
    } 
     
    protected void compileApiResource( String s ) //throws ScriptException
    {
        InputStream is = getClass().getResourceAsStream(s);
        if( is == null )
            throw new IllegalArgumentException( "Script resource not found for:" + s );
        s = RESOURCE_PREFIX + s;
        
        Script script = compile(is, s);
        addApiScript(script, s);

        System.out.println("Compiled script:" + script);
        System.out.println("Metaclass:" + script.getMetaClass());
        System.out.println("The class:" + script.getMetaClass().getTheClass());
        Class scriptClass = script.getMetaClass().getTheClass(); 
        
        // save all current closures into global closures map
        Method[] methods = scriptClass.getMethods();
        for( Method m : methods ) {
            String name = m.getName();
            if( Modifier.isStatic(m.getModifiers()) ) {
                continue;
            }
            if( name.indexOf('$') >= 0 || "run".equals(name) ) {
                continue;
            }
            if( m.getDeclaringClass() != scriptClass ) {
                //System.out.println("Need to skip:" + m);
                continue;
            }
System.out.println("Found method:" + m + "   on type:" + m.getDeclaringClass());
            //globalClosures.put(name, new MethodClosure(scriptObject, name));
            bindings.setVariable(name, new MethodClosure(script, name)); 
        }
        
        // NOTE: Groovy's JSR223 implementation also does the following... but
        //       so far I haven't needed it.  Perhaps because I'm making my 
        //       methods more global in the first place.
                scriptObject.setMetaClass(new DelegatingMetaClass(oldMetaClass) {
                    @Override
                    public Object invokeMethod(Object object, String name, Object args) {
                        if (args == null) {
                            return invokeMethod(object, name, MetaClassHelper.EMPTY_ARRAY);
                        }
                        if (args instanceof Tuple) {
                            return invokeMethod(object, name, ((Tuple) args).toArray());
                        }
                        if (args instanceof Object[]) {
                            return invokeMethod(object, name, (Object[]) args);
                        } else {
                            return invokeMethod(object, name, new Object[]{args});
                        }
                    }

                    @Override
                    public Object invokeMethod(Object object, String name, Object[] args) {
                        try {
                            return super.invokeMethod(object, name, args);
                        } catch (MissingMethodException mme) {
                            return callGlobal(name, args, ctx);
                        }
                    }

                    @Override
                    public Object invokeStaticMethod(Object object, String name, Object[] args) {
                        try {
                            return super.invokeStaticMethod(object, name, args);
                        } catch (MissingMethodException mme) {
                            return callGlobal(name, args, ctx);
                        }
                    }
                });
        
    }
 
    protected String read( Reader in ) throws IOException
    {
        try
            {
            return CharStreams.toString(in);
            }            
        finally
            {
            in.close();
            }
    }

    protected String toLogCategory( String name ) 
    {
        int split = name.lastIndexOf('.');
        if( split > 0 ) {
            name = name.substring(0, split);
        }
        split = name.lastIndexOf('!');
        if( split > 0 ) {
            name = name.substring(split + 1);
        }
        return name;
    }

    protected Script compile( Reader in, String fileName ) //throws ScriptException
    {
        //try
         //   {
            //String script = read(in);
            
            // Make sure the name is ok to be a class name
            if( fileName.indexOf('-') >= 0 ) {
                fileName = fileName.replaceAll("-", "_");
            }
            
            Script result = compiler.parse(in, fileName);
            //if( includeSourceInErrors )
            //    sourceCode.put(result, script);
            return result;
        //    }
        //catch( IOException e ) 
        //    {
        //    throw new ScriptException(e);
        //    }
    }
 
    protected Script compile( InputStream in, String fileName ) //throws ScriptException
    {
        return compile(new InputStreamReader(in), fileName);
    }

    protected Script compile( File f ) //throws ScriptException
    {
        try
            {
            return compile(new FileReader(f), String.valueOf(f));
            } 
        catch( IOException e ) 
            {
            throw new RuntimeException("Error compiling:" + f, e);
            }
    }
    
    protected void compileApiFile( File f ) //throws ScriptException
    {
        String s = FILE_PREFIX + f;
        addApiScript( compile(f), s);
    }    
        
    public void setBinding( String name, Object value )
    {
        life.log("setBinding(" + name + ", " + value + ")");
        bindings.setProperty( name, value );
    }
    
    public Object getBinding( String name )
    {
        return bindings.getProperty(name);
    }
 
    private BindingsProxy localBindings( Binding parent, Object source )
    {
        String scriptName = String.valueOf(source);
        
        BindingsProxy result = new BindingsProxy(parent);
        result.local.put("scriptSource", scriptName);
        result.local.put("log", Log.getLog(toLogCategory(scriptName)));
        return result;
    }
 
    public void initializeApi()
    {
        life.log("initializeApi()");
        for( Script script : api )
            {
            //try
            //    {
                if( log.isDebugEnabled() )
                    log.debug( "evaluating:" + script );
                Object source = sources.get(script); 
                if( log.isDebugEnabled() )
                    log.debug( "Setting script source to:" + source );
                    
                script.setBinding(localBindings(bindings, source));
                life.log("run:" + script + "  source:" + source);
                Object result = script.run(); //eval(localBindings(bindings, source));
                if( log.isDebugEnabled() )
                    log.debug( "result:" + result );
             //   }
            //catch( ScriptException e )
            //    {
            //    if( includeSourceInErrors )
            //        {
            //        throw new RuntimeException( "Error running:" + script 
            //                                    + " from:" + sources.get(script)
            //                                    + " source:\n" + sourceCode.get(script), e );
            //        }
            //    else
            //        {
            //        throw new RuntimeException( "Error running:" + script 
            //                                    + " from:" + sources.get(script), e );
            //        }                                                
            //    }
            }
    }
    
    public void evalResource( String s )
    {
        InputStream is = getClass().getResourceAsStream(s);
        if( is == null )
            throw new IllegalArgumentException( "Script resource not found for:" + s );
        eval(is, s);
    }
    
    public void eval( URL u )
    {
        try
            {
            eval(u.openStream(), String.valueOf(u));
            }
        catch( IOException e )
            {
            throw new RuntimeException( "Error reading:" + u, e );
            }
    }

    public void eval( URI u )
    {    
        try
            {
            eval(u.toURL());
            }
        catch( MalformedURLException e )
            {
            throw new RuntimeException( "Script URI error:" + u, e );
            }
    }
 
    public void eval( File f )
    {
        eval(f.toURI());
    }
    
    public void eval( InputStream is, String s )
    {
        life.log("eval(" + s + ")");
        //try
        //    {            
            if( log.isDebugEnabled() )
                log.debug( "evaluating:" + s );
            Script script = compile(is, s);

            if( log.isDebugEnabled() )
                log.debug( "Setting script source to:" + s );
                                
            int before = bindings.getVariables().size();
    
            //BindingsProxy proxy = localBindings(bindings, s);
            //script.setBinding(proxy);
System.out.println("Running script:" + script + " with bindings:" + bindings.getVariables());            
            Object result = script.run(); //eval(localBindings(bindings, s));
            if( log.isDebugEnabled() )
                log.debug( "result:" + result );                
            
            // Now that we proxy the bindings, we could get rid of this
            // check and let the scripts have their own local bindings. 
            if( before != bindings.getVariables().size() )
                {
                log.warn( "Binding count increased executing:" + s + "  keys:" + bindings.getVariables().keySet() );
                }
        //    }
        //catch( ScriptException e )
        //    {
        //    throw new RuntimeException( "Error running resource:" + s, e );
        //    }        
    }

    private static class BindingsProxy extends Binding //extends AbstractMap<String, Object>
                                       //implements Bindings 
    {
        private Binding delegate;
        private Map local = new HashMap();
        
        public BindingsProxy( Binding delegate ) 
        {
            this.delegate = delegate;
        }

        @Override
        public Object getVariable( String key ) {
            return get(key);
        }
        
        @Override
        public void setVariable( String key, Object value ) {
            //put(key, value);
            delegate.setVariable(key, value);
        }

        public Set keySet() {
            return Sets.union(local.keySet(), delegate.getVariables().keySet());
        }
        
        public Set entrySet() {
            return Sets.union(local.entrySet(), delegate.getVariables().entrySet());
        }        

        public Object get( Object key ) {
            if( local.containsKey(key) ) {
                return local.get(key);
            }
            return delegate.getProperty(String.valueOf(key));
        }

        public Object remove( Object key ) {
            return local.remove(key); //delegate.remove(key);
        }

        public Object put( String key, Object o ) {
            //return delegate.put(key, o);
            Object result = delegate.getVariable(key);
            delegate.setVariable(key, o);
            return result;
        }
    }    
}

*/
