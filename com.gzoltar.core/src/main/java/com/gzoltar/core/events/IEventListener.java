package com.gzoltar.core.events;

import com.gzoltar.core.model.Node.Type;

public interface IEventListener {

  void endTransaction(String transactionName, boolean[] activity, boolean isError);

  void endTransaction(String transactionName, boolean[] activity, int hashCode, boolean isError);

  void addNode(int id, String name, Type type, int parentId);

  void addProbe(int id, int nodeId);

  void endSession();
}