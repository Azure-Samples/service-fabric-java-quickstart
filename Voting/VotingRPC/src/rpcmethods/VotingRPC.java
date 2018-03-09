package rpcmethods; 

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.HashMap;

import microsoft.servicefabric.services.remoting.Service;

public interface VotingRPC extends Service {
	CompletableFuture<HashMap<String, String>> getList();

	CompletableFuture<Integer> addItem(String itemToAdd);

	CompletableFuture<Integer> removeItem(String itemToRemove);
}