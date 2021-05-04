# CareLinkJavaClient
> Experimental Medtronic CareLink Client in Java

Java library, which can be used for retrieving data from Medtronic CareLink of online CGM and insulin pump device data uploads (Guardian Connect, MiniMed 7xxG) inside and outside of US. Originally it was started as a Guardian Connect CGM data from CareLink to Nightscout alternative for Nightscout MiniMed Connect, because Medtronic started to block requests from Heroku. Later it turned out that the core Nightscout MiniMed Connect logic can be also used for 7xxG pumps with some modifications based on the US online CareLink Connect webapp (770G is still working). The US data retrieval logic was also working outside the US with the 780G pumps too, although the online CareLink Connect webapp outside US doesn't support the new generation pumps. Finally the code was extended to handle the 7xxG devices too both inside and outside the US.

## Status
**The development is in a very early stage!**

## Supported devices
- Medtronic Guardian Connect CGM
- Medtronic MiniMed 770G pump
- Medtronic MiniMed 780G pump
- Other Medtronic MiniMed 7xxG pumps???

## Features
- Login to CareLink and provide access token for CareLink API calls 
- Some basic CareLink APIs: get user data, get user profile, get country settings, get last 24 hours, get recent data from CareLink Cloud
- Wrapper method for getting data uploaded by Medtronic BLE devices of the last 24 hours
- CareLink Client CLI 

## Limitations
- **CareLink MFA is not supported!!!**
- Notification messages are in English

## Requirements
- CareLink account **with MFA NOT ENABLED!**:
    - Guardian Connect CGM outside US: patient or care partner account
    - Guardian Connect CGM inside US: **not tested yet!** (possibly a care partner account)
    - 7xxG pump outside US: care partner account (same as for Medtronic CareLink Connect app)
    - 7xxG pump inside US: care partner account (same as for Medtronic CareLink Connect app)
- Runtime: Java 1.8
- External libraries used:
    - Kotlin 1.4
    - OkIO 2.8
    - OkHttp 4.9
    - Gson 2.8
    - Commons CLI 1.4

## How to use

### Download and install [latest release](https://github.com/benceszasz/CareLinkJavaClient/releases)

### Get data of last 24 hours using Java
    CareLinkClient client;
    RecentData recentData;

    client = new CareLinkClient("carelink_username", "carelink_password", "carelink_country_code");
    if(client.login()) {
        recentData = client.getRecentData();
    }

### Download last 24 hours using CLI
    java -jar carelink-client.jar -u carelink_username -p carelink_password -c carelink_country_code -d

### Get CLI options
    java -jar carelink-client.jar

## Credits
CareLink data download core logic is based on the [Nightscout MiniMed Connect to Nightscout](https://github.com/nightscout/minimed-connect-to-nightscout)

## Disclaimer And Warning
This project is intended for educational and informational purposes only. It relies on a series of fragile components and assumptions, any of which may break at any time. It is not FDA approved and should not be used to make medical decisions. It is neither affiliated with nor endorsed by Medtronic, and may violate their Terms of Service. Use of this code is without warranty or formal support of any kind.

## License

[agpl-3]: http://www.gnu.org/licenses/agpl-3.0.txt

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
    
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
