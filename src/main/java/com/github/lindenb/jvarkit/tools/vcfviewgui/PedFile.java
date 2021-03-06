/*
The MIT License (MIT)

Copyright (c) 2017 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/
package com.github.lindenb.jvarkit.tools.vcfviewgui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;


import htsjdk.samtools.util.CloserUtil;

public class PedFile implements Iterable<PedFile.Sample>
	{
    private static final Logger LOG= Logger.getLogger("PedFile");

	static final String EXTENSION=".ped";
	public enum Sex {Male,Female,Unknown};
	public enum Status {Affected,Unaffected,Unknown};
	private final Map<String,Sample> name2sample=new HashMap<>();
	
	
	public interface Sample
		{
		public String getFamily();
		public String getName();
		public default boolean hasFather() { return getFather()!=null;}
		public default boolean hasMother() { return getMother()!=null;}
		public default String getFatherName() { return hasFather()?getFather().getName():null;}
		public default String getMotherName() { return hasMother()?getMother().getName():null;}
		public Sample getFather();
		public Sample getMother();
		public Sex getSex();/*RIP george Michael */
		public Status getStatus();
		public default boolean isAffected() { return getStatus()==Status.Affected;}
		public default boolean isUnaffected() { return getStatus()==Status.Unaffected;}
		}
	
	private static final PedFile EMPTY=new PedFile();
	public static PedFile getEmptyInstance()
		{
		return EMPTY;
		}
	
	public boolean isEmpty()
		{
		return this.name2sample.isEmpty();
		}
	
	@Override
	public Iterator<Sample> iterator()
		{
		return name2sample.values().iterator();
		}
	
	public Set<String> getSampleNames()
		{
		return Collections.unmodifiableSet(this.name2sample.keySet());
		}	
	
	public boolean has(final String sample) {
		return this.name2sample.containsKey(sample);
		}
	
	public Sample get(final String sample) {
		return this.name2sample.get(sample);
		}
	
	private class SampleImpl implements PedFile.Sample
		{
		String family;
		String name;
		String father=null;
		String mother=null;
		Sex sex=Sex.Unknown;
		Status status=Status.Unknown;
		@Override
		public String getFamily() { return family; }
		@Override
		public String getName() { return name; }
		@Override
		public Sample getFather() { return father==null?null:PedFile.this.name2sample.get(father); }
		@Override
		public Sample getMother() { return mother==null?null:PedFile.this.name2sample.get(mother); }
		@Override
		public Sex getSex() { return sex; }
		@Override
		public Status getStatus() { return status; }
		@Override
		public boolean equals(Object obj)
			{
			if(obj==this) return true;
			if(obj==null || !(obj instanceof SampleImpl)) return false;
			final SampleImpl other=(SampleImpl)obj;
			return this.name.equals(other.name) && 
					this.family.equals(other.family);
			}
		@Override
		public int hashCode()
			{
			return this.name.hashCode();
			}
		@Override
		public String toString()
			{
			return getName();
			}
		}
	
	
	public static PedFile load(final BufferedReader r) throws IOException
		{
		String line;
		final Pattern delim=Pattern.compile("[\t ]+");
		final PedFile ped=new PedFile();
		while((line=r.readLine())!=null)
			{
			if(line.trim().isEmpty() || line.startsWith("#")) continue;
			final String tokens[]=delim.split(line);
			if(tokens.length<4) throw new IOException("Not enought column in "+line);
			final SampleImpl sample=ped.new SampleImpl();
			sample.family=tokens[0];
			sample.name=tokens[1];
			if(sample.name.isEmpty()) throw new IOException("Sample name cannot be empty");
			sample.father=(tokens[2].isEmpty() || tokens[2].equals("0")?null:tokens[2]);
			sample.mother=(tokens[3].isEmpty() || tokens[3].equals("0")?null:tokens[3]);
			if(tokens.length>4) {
				if(tokens[4].toLowerCase().equals("M") || tokens[4].equals("1"))
					{
					sample.sex = Sex.Male;
					}
				else if(tokens[4].toLowerCase().equals("F") || tokens[4].equals("2"))
					{
					sample.sex = Sex.Female;
					}
				else
					{
					sample.sex = Sex.Unknown;
					}
				}
			if(tokens.length>5) {
				if(tokens[5].equals("1"))
					{
					sample.status = Status.Affected;
					}
				else if(tokens[5].equals("0"))
					{
					sample.status = Status.Unaffected;
					}
				else
					{
					sample.status = Status.Unknown;
					}
				}
			if(ped.name2sample.containsKey(sample.getName())) {
				throw new IOException("Duplicate sample "+sample);
				}
			ped.name2sample.put(sample.getName(),sample);
			}
			
		return ped;
		}
	
	public static PedFile load(final File file) throws IOException
		{
		FileReader r=null;
		try {
			LOG.info("reading "+file);
			r=new FileReader(file);
			return load(new BufferedReader(r));
		} finally
		{
		CloserUtil.close(r);
		}
		
		}		
	}
