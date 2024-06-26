const PROXY_CONFIG = {
    "/api/**": {
        "target": "http://localhost:8080/sdnnt/api",
        "changeOrigin": true,
        "secure": false,
        "logLevel": "debug",
		"pathRewrite": {
      		"^/api":""
    	},
        "onProxyRes": function(pr, req, res) {
			console.log("Intercepting request");	
            if (pr.headers['set-cookie']) {
				console.log("Replacing cookie "+pr.headers['set-cookie']);
                const cookies = pr.headers['set-cookie'].map(cookie => 
                    cookie.replace(/\/sdnnt/gi, '/')
                );
                pr.headers['set-cookie'] = cookies;
				console.log("Replaced cookie "+pr.headers['set-cookie']);
              }
        }
   },
   "/assets/**": {
        "target": "http://localhost:8080/sdnnt/assets/",
        "changeOrigin": true,
        "secure": false,
        "logLevel": "debug",
		"pathRewrite": {
            "^/assets":""
    	},
        "onProxyRes": function(pr, req, res) {
			console.log("Intercepting request");	
            if (pr.headers['set-cookie']) {
				console.log("Replacing cookie "+pr.headers['set-cookie']);
                const cookies = pr.headers['set-cookie'].map(cookie => 
                    cookie.replace(/\/sdnnt/gi, '/')
                );
                pr.headers['set-cookie'] = cookies;
				console.log("Replaced cookie "+pr.headers['set-cookie']);
              }
        }
   }

};

module.exports = PROXY_CONFIG;