/** 
 * Author:  anthony.fodor@gmail.com    
 * This code is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version,
* provided that any use properly credits the author.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details at http://www.gnu.org * * */



package biolockj.util;

public class TabReader
{
    private String inString;
    
    public TabReader(String inString ) 
    {
		if ( inString == null )
			throw new NullPointerException();
		
        this.inString = inString;
    }
    
    public String nextToken()
    {
    	return getNext();
    }
    
    public String getNext()
    {
        if ( inString == null ) 
            throw new RuntimeException("Error!  No more to get");
        
        int index = inString.indexOf('\t');
        String returnString = "";
        
        if ( index == -1 ) 
        {
            returnString = inString;
            inString = null;
        }
        else if ( index == 0 ) 
        {
            if ( inString.length() > 1 ) 
                inString = inString.substring(1);
            else
                inString = "";
        }
        else
        {
            // there's a tab, but not at the first position
            returnString = inString.substring(0, index );
            
            inString = inString.substring( index );
            
            if ( inString.length() <= 1 ) 
                inString = "";
            else
                inString = inString.substring(1);
        }
        
        return returnString;
    }
    
    public boolean hasMore()
    {
        return this.inString != null;
    }	
}
