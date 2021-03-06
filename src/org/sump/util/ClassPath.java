/*
 *  Copyright (C) 2012 John Pritchard
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or (at
 *  your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 */
package org.sump.util;

import org.sump.analyzer.Configurable;
import org.sump.analyzer.Diagram;
import org.sump.analyzer.devices.DeviceController;
import org.sump.analyzer.tools.Tool;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scan class path for tools, devices, etc..
 */
public final class ClassPath 
    extends Object
{
    private final static Class DeviceControllerClass = DeviceController.class;
    private final static Class ToolClass = Tool.class;
    private final static Class ConfigurableClass = Configurable.class;


    private final DeviceController[] controllers;
    private final Tool[] tools;
    private final Configurable[] configurables;


    public ClassPath(){
        super();
        final List<DeviceController> controllers = new LinkedList();
        final List<Tool> tools = new LinkedList();
        final List<Configurable> configurables = new LinkedList();


        final StringTokenizer strtok = new StringTokenizer(System.getProperty("java.class.path"),System.getProperty("path.separator"));
        while (strtok.hasMoreTokens()){
            String pel = strtok.nextToken();
            try {
                JarFile jf = new JarFile(pel);

                Enumeration<JarEntry> je = jf.entries();

                while(je.hasMoreElements()) {

                    String entryname = je.nextElement().getName();

                    if (entryname.endsWith(".class")) {

                        String classname = entryname.substring(0, entryname.indexOf(".class")).replace('/', '.');
                        try {
                            final Class clas = Class.forName(classname);
                            final int type = clas.getModifiers();

                            if ((!clas.isInterface()) && (!Modifier.isAbstract(type))){
                                /*
                                 * Instantiate each class at most once
                                 */
                                if (DeviceControllerClass.isAssignableFrom(clas)){

                                    DeviceController controller = (DeviceController)clas.newInstance();

                                    controllers.add(controller);

                                    System.err.printf("Loaded Controller %s%n",clas.getName());

                                    if (controller instanceof Tool)
                                        tools.add((Tool)controller);

                                    if (controller instanceof Configurable)
                                        configurables.add((Configurable)controller);

                                }
                                else if (ToolClass.isAssignableFrom(clas)){

                                    Tool tool = (Tool)clas.newInstance();

                                    tools.add(tool);

                                    System.err.printf("Loaded Tool %s%n",clas.getName());

                                    if (tool instanceof Configurable)
                                        configurables.add((Configurable)tool);

                                }
                                else if (ConfigurableClass.isAssignableFrom(clas)){

                                    Configurable configurable = (Configurable)clas.newInstance();

                                    configurables.add(configurable);

                                    System.err.printf("Loaded Configurable %s%n",clas.getName());
                                }
                            }
                        }
                        catch (Exception e) {
                            synchronized(System.err){
                                System.err.printf("Error loading class '%s'%n",classname);
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.controllers = controllers.toArray(new DeviceController[0]);
        this.tools = tools.toArray(new Tool[0]);
        this.configurables = configurables.toArray(new Configurable[0]);
    }


    public DeviceController[] controllers(){
        return this.controllers.clone();
    }
    public Tool[] tools(){
        return this.tools.clone();
    }
    public Configurable[] configurables(){
        return this.configurables.clone();
    }
    public Tool[] tools(Frame frame){

        for (Tool tool: this.tools){

            tool.init(frame);
        }
        return this.tools.clone();
    }
    public Diagram getDiagram(){

        for (Configurable c: this.configurables){

            if (c instanceof Diagram)
                return (Diagram)c;
        }
        throw new Error();
    }
}
