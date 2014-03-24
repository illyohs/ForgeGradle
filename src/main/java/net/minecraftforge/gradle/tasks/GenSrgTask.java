package net.minecraftforge.gradle.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;

import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.tasks.abstractutil.CachedTask;

import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.io.Files;

public class GenSrgTask extends CachedTask
{

    @InputFile
    private DelayedFile inSrg;

    @InputFile
    private DelayedFile methodsCsv;

    @InputFile
    private DelayedFile fieldsCsv;

    @Cached
    @OutputFile
    private DelayedFile notchToMcp;

    @Cached
    @OutputFile
    private DelayedFile mcpToSrg;
    
    @Cached
    @OutputFile
    private DelayedFile mcpToNotch;
    
    @TaskAction
    public void doTask() throws IOException
    {
        HashMap<String, String> methods = new HashMap<String, String>();
        HashMap<String, String> fields = new HashMap<String, String>();
        
        // read methods
        CSVReader csvReader = RemapSourcesTask.getReader(getMethodsCsv());
        for (String[] s : csvReader.readAll())
        {
            methods.put(s[0], s[1]);
        }

        // read fields
        csvReader = RemapSourcesTask.getReader(getFieldsCsv());
        for (String[] s : csvReader.readAll())
        {
            fields.put(s[0], s[1]);
        }
        
        
        File deobfFile = getNotchToMcp();
        File reobfFile = getMcpToSrg();

        // verify files...
        if (!deobfFile.exists())
        {
            deobfFile.getParentFile().mkdirs();
            deobfFile.createNewFile();
        }
        if (!reobfFile.exists())
        {
            reobfFile.getParentFile().mkdirs();
            reobfFile.createNewFile();
        }
        
        // create streams
        BufferedReader srgIn = Files.newReader(getInSrg(), Charset.defaultCharset());
        BufferedWriter notch2Mcp = Files.newWriter(getNotchToMcp(), Charset.defaultCharset());
        BufferedWriter mcpToSrg = Files.newWriter(getMcpToSrg(), Charset.defaultCharset());
        BufferedWriter mcpToNotch = Files.newWriter(getMcpToNotch(), Charset.defaultCharset());
        
        // IN
        // notch -> srg
        
        // deobf
        // notch -> mcp
        
        // reobf
        // mcp -> srg
        
        String line, temp, in, out;
        String[] split;
        while ((line = srgIn.readLine()) != null)
        {
            if (line.startsWith("PK:"))
            {
                // nobody cares about the packages.
                notch2Mcp.write(line);
                notch2Mcp.newLine();
                
                mcpToSrg.write(line);
                mcpToSrg.newLine();
                
                mcpToNotch.write(line);
                mcpToNotch.newLine();
            }
            else if (line.startsWith("CL:"))
            {
                // deobf:  no change here...
                notch2Mcp.write(line);
                notch2Mcp.newLine();
                
                // reobf: same classes on both sides.
                split = line.split(" "); // 0=type  1=notch  2=srg=mcp
                mcpToSrg.write("CL: "+split[2]+" "+split[2]);
                mcpToSrg.newLine();
                
                // output is notch
                mcpToNotch.write("CL: "+split[2]+" "+split[1]);
                mcpToNotch.newLine();
            }
            else if (line.startsWith("FD:"))
            {
                // deobf: need to rename that method.
                split = line.split(" "); // 0=type  1=notch  2=srg
                
                temp = split[2].substring(split[2].lastIndexOf('/')+1);
                out = split[2];
                
                if (fields.containsKey(temp))
                    out = split[2].replace(temp, fields.get(temp));
                
                notch2Mcp.write("FD: "+split[1]+" "+out);
                notch2Mcp.newLine();
                
                // reobf: reverse too
                mcpToSrg.write("FD: "+out+" "+split[2]);
                mcpToSrg.newLine();
                
                // output is notch
                mcpToNotch.write("FD: "+out+" "+split[1]);
                mcpToNotch.newLine();
            }
            else if (line.startsWith("MD:"))
            {
                // deobf: rename that method.
                split = line.split(" "); // 0=type  1-2=notch  3-4=srg
                temp = split[3].substring(split[3].lastIndexOf('/')+1);
                
                in = split[1] + " " + split[2]; // notch
                out = split[3] + " " + split[4]; // srg
                
                if (methods.containsKey(temp))
                    out = out.replace(temp, methods.get(temp)); // now MCP
                
                notch2Mcp.write("MD: "+in+" "+out);
                notch2Mcp.newLine();
                
                // reobf reverse too
                mcpToSrg.write("MD: "+out+" "+split[3]+" "+split[4]);
                mcpToSrg.newLine();
                
                // output is notch
                mcpToNotch.write("MD: "+out+" "+split[1]+" "+split[2]);
                mcpToNotch.newLine();
            }
        }
        
        srgIn.close();
        
        notch2Mcp.flush();
        notch2Mcp.close();
        
        mcpToSrg.flush();
        mcpToSrg.close();
        
        mcpToNotch.flush();
        mcpToNotch.close();
    }

    public File getInSrg()
    {
        return inSrg.call();
    }

    public void setInSrg(DelayedFile inSrg)
    {
        this.inSrg = inSrg;
    }

    public File getMethodsCsv()
    {
        return methodsCsv.call();
    }

    public void setMethodsCsv(DelayedFile methodsCsv)
    {
        this.methodsCsv = methodsCsv;
    }

    public File getFieldsCsv()
    {
        return fieldsCsv.call();
    }

    public void setFieldsCsv(DelayedFile fieldsCsv)
    {
        this.fieldsCsv = fieldsCsv;
    }

    public File getNotchToMcp()
    {
        return notchToMcp.call();
    }

    public void setNotchToMcp(DelayedFile deobfSrg)
    {
        this.notchToMcp = deobfSrg;
    }

    public File getMcpToSrg()
    {
        return mcpToSrg.call();
    }

    public void setMcpToSrg(DelayedFile reobfSrg)
    {
        this.mcpToSrg = reobfSrg;
    }
    
    public File getMcpToNotch()
    {
        return mcpToNotch.call();
    }

    public void setMcpToNotch(DelayedFile reobfSrg)
    {
        this.mcpToNotch = reobfSrg;
    }
}
