## Build image steps

1. Build builder image. Use following command: `docker build -t sdnnt-builder:1.04 .`
2. download s2i utility from following site: https://github.com/openshift/source-to-image/releases
3. Build image from source. Use following command:  `s2i build --loglevel=5 --incremental=true --exclude="" https://github.com/NLCR/SeznamDNNT.git sdnnt-builder:1.04  sdnnt:latest sdnnt`
	
	