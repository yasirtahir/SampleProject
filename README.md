Sample Project
=======================

### Implement an autocompleting travel search form
On the form, the user is required to enter the start and end location and the date of their trip. The start and end location need to be automatically completed using a list of known locations that can be requested through a JSON API invocation. When displaying matches, they should be ordered by distance to the user's current location. Date entry should be facilitated by a date entry widget. A "search" button should be available when the form has been completely filled out. Tapping the "search" button should display a "Search is not yet implemented" message to the user.


![wireframe](wireframe.png?raw=true)



The app use the position API endpoint that can be found here: `http://api.goeuro.com/api/v2/position/suggest/{locale}/{term}`

##### For example:
 http://api.goeuro.com/api/v2/position/suggest/en/hamburg

Where `{term}` is the string that the user has entered so far.

