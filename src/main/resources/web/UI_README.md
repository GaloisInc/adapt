Adapt Graph UI
==============


Concept
-------

This UI is meant to be an interactive exploration tool for visualizing graph data. It assumes there is a server available via a REST API which can provide the graph data. The UI is meant to be customized in appearance and query capability via definitions found in the corresponding file `ui_language.js`. The UI is intended to be query-language agnostic and able to query a graph database using any query language.


Authors
-------

Ryan Wright. Galois, Inc. ryan@galois.com

Alec Therialt. Galois, Inc. atheriault@galois.com

Will Maxwell. Oregon State University. wmaxwell90@gmail.com 


External Libraries
---------------

This UI appreciatively makes use of the fine work of the following libraries:

* Vis.js - http://visjs.org
* jQuery - https://jquery.com
* underscore.js - https://underscorejs.org
* Ionicons - https://ionicons.com
* jscolor - http://jscolor.com


Features
--------
* Query any source of graph data over a REST API.
* Render nodes in browser according to customizable parameters (icon, color, size, etc.) applied to different node types
* Right-click context menu, populated by customizable queries to be executed from the chosen node.
* Rendering of "synthetic edges" with dotted arrows which result from using the context menu, but do not exist in the underlying data.
* Collapse several nodes into a single node.
* Change the color of nodes using the color swatch next to the "query" button.
* A log of all updates to the graph is maintained and able to be reversed (undo), replayed (redo), downloaded, or shared.
* Saving checkpoints in the UI update log to annotate certain states during exploration.
* Pin nodes in place (removes them from the layout rendering engine) by holding shift, and clicking+holding on a node until it flashes black. A pinned node will have a drop shadow.
* History of each node addition/removal/modification is saved in a linear event log. This allows undoing/redoing query results.
* Checkpoints can be saved and named at any point in the event history.
* The event history can be saved to disk and loaded again on another browser or computer.
* A collapsed event log which represents only the current view (not the rendering history) can be saved by holding shift when clicking the download button.
* Click-and-hold a node to dump its JSON to the javascript console.


Getting Started
---------------

1. Edit the `graph.html` file. Near the top of the file, inline comments identify a handful of variables which should be edited to suit the environment this will be run in. These variables define how to fetch nodes and edges. (This is the only _essential_ step.)
2. Edit `ui_langiage.js` to define appearance of node types as desired (in the `node_appearance` variable), along with the context-menu queries available for each node type (in the `predicates` variable).
3. Navigate to the `graph.html` file in a web broswer. (ensure no warnings are shown in the browser's javascript console, or it may indicate a problem with steps #1 or #2.)
4. Enter a query in the text box. If `ui_language.js` has defined `starting_queries`, you can use the drop-down arrow to choose a query. Pressing enter or clicking "Query" will execute the entered query. Queries entered in this box should return nodes. OPTIONAL: holding shift while executing a query will log the result to the javascript console instead of rendering it (the query box will flash green as confirmation).
5. A successful query result will render nodes and edges in the main section of the browser window. Edges will only be rendered if the nodes on both ends of the edge appear in the browser window.
6. Graph layout is done via an iterative physics simulation. This simulation runs for one second after a query. This may be insufficient time to layout the results. The play button at the top of the window can be clicked to allow iterating the layout simulation to any arbitrary amount of time. Click the pause button once the graph is sufficiently laid out.
7. Right click a node to see the context menu of available queries specific to that node. Querying via the context menu will result in "synthetic edges" rendered with dashes to indicate that they do not correspond to an edge in the underlying data.
8. Back up or replay node rendering by clicking the arrows on each side of the play button at the top of the window. The single arrow with a veritcal line jumps one step forward or backward in the rendering sequence. The double arrows jump to the previous/next checkpoint.
9. Download the current view (and all history) with the download button. This history file can be saved and uploaded into a new browser to show the current layout, and corresponding exploration history.


Known Issues
------------

* This tool has only ever been tested with Google Chrome. YMMV in other browsers.
* Coloring of edges is broken in the underlying `vis.js` library.
* This tool was initially created with Gremlin as the query language. Some vestigal gremlin assumptions may still exist.
