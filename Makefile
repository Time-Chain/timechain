# This Makefile is meant to be used by people that do not usually work
# with Go source code. If you know what GOPATH is then you probably
# don't need to bother with make.

.PHONY: all test clean

GOBIN = ./build/bin
GO ?= latest
GORUN = env GO111MODULE=on go run

all:
	$(GORUN) build/ci.go install ./cmd/geth
	strip $(GOBIN)/geth
	cp $(GOBIN)/geth docs/static-nodes.json docs/time.json  .
	tar zcvf TimeChain-linux-v1.1.0.tar.gz geth static-nodes.json time.json
	@echo "Done building."
	@echo "Run \"$(GOBIN)/geth\" to launch."

clean:
	env GO111MODULE=on go clean -cache
	rm -fr build/_workspace/pkg/ $(GOBIN)/*
