#Tellstick Binding

This is an OpenHAB binding for Tellstick devices produced by Telldus, a Swedish company based in Lund.

The original Tellstick focused on controlling 433 MHz devices like switches, dimmers and reading sensors from different brands. <br>
Many of the supported devices are cheaper and "low-end" and support have been made by reverse engineer the transmission protocols. <br>
All of these 433 MHz devices is one-way, so some versions of the Tellstick monitoring the air to keep the state of all devices. 
  
The latest versions have also implemented Z-Wave as transmission protocol which open up for more robust transmission due two-ways communication. 
 
## Supported Things

This binding implements two different API:  
**1)** *Telldus Core* which is a local only interface supported by USB based device. <br>
**2)** *Telldus Live* which is a REST based cloud service maintained by Telldus. <br>
3) (According to [Telldus](http://developer.telldus.com/blog/2016/01/21/local-api-for-tellstick-znet-lite-beta) are they working with a local REST based API for the new Z-Wave devices. This is currently **NOT** supported by this binding.)

Depending on your Tellstick model different API methods is available: 

<table>
<tr><td><b>Model</b></td> <td><b>Telldus Core</b></td> <td><b>Telldus Live</b></td> <td><b>Verified working with openHAB</b></td></tr>
<tr><td>Tellstick Basic</td><td>X</td><td>X</td></tr>
<tr><td>Tellstick Duo</td><td>X</td><td>X</td><td>X</td></tr>
<tr><td>Tellstick Net</td><td></td><td>X</td></tr>
<tr><td>Tellstick ZNet Lite</td><td></td><td>X</td></tr>
<tr><td>Tellstick ZNet Pro</td><td></td><td>X</td></tr>
</table>


This binding supports the following thing types:

TBD

## Binding Configuration

No binding configuration required.

## Discovery

TBD

## Thing Configuration

TBD

## Channels

TBD

## Full Example

TBD
