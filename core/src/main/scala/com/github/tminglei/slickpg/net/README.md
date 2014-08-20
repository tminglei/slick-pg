Supported Inet Oper/Functions
------------------------------

| Slick Oper/Function | PG Oper/Function |       Description       |                Example                | Result |
| ------------------- | ---------------- | ----------------------- | ------------------------------------- | ------ |
| <<                  | <<               | is contained by         | inet '192.168.1.5' << inet '192.168.1/24' |   t    |
| <<=                 | <<=              | is contained by or equals | inet '192.168.1/24' <<= inet '192.168.1/24' |   t    |
| >>                  | >>               | contains   						 | inet '192.168.1/24' >> inet '192.168.1.5' |   t    |
| >>=                 | >>=              | contains or equals 		 | inet '192.168.1/24' >>= inet '192.168.1/24' |   t    |
| &&                  | &&               | contains or is contained by | inet '192.168.1/24' && inet '192.168.1.80/28' |   t    |
| ~                   | ~                | bitwise NOT        		 | ~ inet '192.168.1.6' 								 | 63.87.254.249 |
| &                   | &                | bitwise AND       			 | inet '192.168.1.6' & inet '0.0.0.255' | 0.0.0.6 |
| &#124;              | &#124;           | bitwise OR 						 | inet '192.168.1.6' &#124; inet '0.0.0.255' | 192.168.1.255 |
| +                   | +                | addition  							 | inet '192.168.1.6' + 25 							 | 192.168.1.31 |
| -                   | -                | subtraction          	 | inet '192.168.1.43' - 36							 | 192.168.1.7 |
| --                  | --               | subtraction             | inet '192.168.1.43' - inet '192.168.1.19' | 24 |
| abbrev              | abbrev           | abbreviated display format as text | abbrev(inet '10.1.0.0/16') | 10.1.0.0/16 |
| broadcast           | broadcast        | broadcast address for network | broadcast('192.168.1.5/24')		 | 192.168.1.255/24 |
| family              | family           | extract family of address; 4 for IPv4, 6 for IPv6 | family('::1') |  6   |
| host                | host             | extract IP address as text    | host('192.168.1.5/24')   	| 192.168.1.5 |
| hostmask            | hostmask         | construct host mask for network | hostmask('192.168.23.20/30')	 | 0.0.0.3|
| masklen             | masklen          | extract netmask length  | masklen('192.168.1.5/24')				     |   24   |
| netmask             | netmask          | construct netmask for network | netmask('192.168.1.5/24')    | 255.255.255.0 |
| network             | network          | extract network part of address | netmask('192.168.1.5/24')  | 192.168.1.0/24 |
| setMasklen          | set_masklen      | set netmask length    	 | set_masklen('192.168.1.5/24', 16)     | 192.168.1.5/16 |
| text                | text             | extract IP address and netmask length as text | text(inet '192.168.1.5') | 192.168.1.5/32 |

Supported MacAddr Oper/Functions
--------------------------------
| Slick Oper/Function | PG Oper/Function |       Description       |                Example                | Result |
| ------------------- | ---------------- | ----------------------- | ------------------------------------- | ------ |
| ~                   | ~                | bitwise NOT        		 | ~ macaddr '12:34:56:78:90:ab' 				 | ed:cb:a9:87:6f:54 |
| &                   | &                | bitwise AND       			 | madaddr '12:34:56:78:90:ab' & macaddr '08:00:2b:01:02:03' | 00:00:02:00:00:03 |
| &#124;              | &#124;           | bitwise OR 						 | madaddr '12:34:56:78:90:ab' &#124; macaddr '08:00:2b:01:02:03' | 1a:34:7f:79:92:ab |
| trunc               | trunc            | set last 3 bytes to zero | trunc(macaddr '12:34:56:78:90:ab')   | 12:34:56:00:00:00 |
