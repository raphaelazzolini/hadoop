/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.protocolPB;

import java.io.Closeable;
import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.proto.HdfsServerProtos.NamenodeCommandProto;
import org.apache.hadoop.hdfs.protocol.proto.HdfsServerProtos.VersionRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.EndCheckpointRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.ErrorReportRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.GetBlockKeysRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.GetBlockKeysResponseProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.GetBlocksRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.GetEditLogManifestRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.GetMostRecentCheckpointTxIdRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.GetNextSPSPathRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.GetNextSPSPathResponseProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.GetTransactionIdRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.IsRollingUpgradeRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.IsRollingUpgradeResponseProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.IsUpgradeFinalizedRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.IsUpgradeFinalizedResponseProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.RegisterRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.RollEditLogRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.NamenodeProtocolProtos.StartCheckpointRequestProto;
import org.apache.hadoop.hdfs.security.token.block.ExportedBlockKeys;
import org.apache.hadoop.hdfs.server.namenode.CheckpointSignature;
import org.apache.hadoop.hdfs.server.protocol.BlocksWithLocations;
import org.apache.hadoop.hdfs.server.protocol.NamenodeCommand;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.NamenodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;
import org.apache.hadoop.hdfs.server.protocol.RemoteEditLogManifest;
import org.apache.hadoop.ipc.ProtocolMetaInterface;
import org.apache.hadoop.ipc.ProtocolTranslator;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RpcClientUtil;
import org.apache.hadoop.thirdparty.protobuf.RpcController;

import static org.apache.hadoop.ipc.internal.ShadedProtobufHelper.ipc;

/**
 * This class is the client side translator to translate the requests made on
 * {@link NamenodeProtocol} interfaces to the RPC server implementing
 * {@link NamenodeProtocolPB}.
 */
@InterfaceAudience.Private
@InterfaceStability.Stable
public class NamenodeProtocolTranslatorPB implements NamenodeProtocol,
    ProtocolMetaInterface, Closeable, ProtocolTranslator {
  /** RpcController is not used and hence is set to null */
  private final static RpcController NULL_CONTROLLER = null;
  
  /*
   * Protobuf requests with no parameters instantiated only once
   */
  private static final GetBlockKeysRequestProto VOID_GET_BLOCKKEYS_REQUEST = 
      GetBlockKeysRequestProto.newBuilder().build();
  private static final GetTransactionIdRequestProto VOID_GET_TRANSACTIONID_REQUEST = 
      GetTransactionIdRequestProto.newBuilder().build();
  private static final RollEditLogRequestProto VOID_ROLL_EDITLOG_REQUEST = 
      RollEditLogRequestProto.newBuilder().build();
  private static final VersionRequestProto VOID_VERSION_REQUEST = 
      VersionRequestProto.newBuilder().build();

  final private NamenodeProtocolPB rpcProxy;
  
  public NamenodeProtocolTranslatorPB(NamenodeProtocolPB rpcProxy) {
    this.rpcProxy = rpcProxy;
  }

  @Override
  public void close() {
    RPC.stopProxy(rpcProxy);
  }

  @Override
  public Object getUnderlyingProxyObject() {
    return rpcProxy;
  }

  @Override
  public BlocksWithLocations getBlocks(DatanodeInfo datanode, long size, long
      minBlockSize, long timeInterval)
      throws IOException {
    GetBlocksRequestProto req = GetBlocksRequestProto.newBuilder()
        .setDatanode(PBHelperClient.convert((DatanodeID)datanode)).setSize(size)
        .setMinBlockSize(minBlockSize).setTimeInterval(timeInterval).build();
    return PBHelper.convert(ipc(() -> rpcProxy.getBlocks(NULL_CONTROLLER, req)
        .getBlocks()));
  }

  @Override
  public ExportedBlockKeys getBlockKeys() throws IOException {
    GetBlockKeysResponseProto rsp = ipc(() -> rpcProxy.getBlockKeys(NULL_CONTROLLER,
        VOID_GET_BLOCKKEYS_REQUEST));
    return rsp.hasKeys() ? PBHelper.convert(rsp.getKeys()) : null;
  }

  @Override
  public long getTransactionID() throws IOException {
    return ipc(() -> rpcProxy.getTransactionId(NULL_CONTROLLER,
        VOID_GET_TRANSACTIONID_REQUEST).getTxId());
  }

  @Override
  public long getMostRecentCheckpointTxId() throws IOException {
    return ipc(() -> rpcProxy.getMostRecentCheckpointTxId(NULL_CONTROLLER,
        GetMostRecentCheckpointTxIdRequestProto.getDefaultInstance()).getTxId());
  }

  @Override
  public CheckpointSignature rollEditLog() throws IOException {
    return PBHelper.convert(ipc(() -> rpcProxy.rollEditLog(NULL_CONTROLLER,
        VOID_ROLL_EDITLOG_REQUEST).getSignature()));
  }

  @Override
  public NamespaceInfo versionRequest() throws IOException {
    return PBHelper.convert(ipc(() -> rpcProxy.versionRequest(NULL_CONTROLLER,
        VOID_VERSION_REQUEST).getInfo()));
  }

  @Override
  public void errorReport(NamenodeRegistration registration, int errorCode,
      String msg) throws IOException {
    ErrorReportRequestProto req = ErrorReportRequestProto.newBuilder()
        .setErrorCode(errorCode).setMsg(msg)
        .setRegistration(PBHelper.convert(registration)).build();
    ipc(() -> rpcProxy.errorReport(NULL_CONTROLLER, req));
  }

  @Override
  public NamenodeRegistration registerSubordinateNamenode(
      NamenodeRegistration registration) throws IOException {
    RegisterRequestProto req = RegisterRequestProto.newBuilder()
        .setRegistration(PBHelper.convert(registration)).build();
    return PBHelper.convert(
        ipc(() -> rpcProxy.registerSubordinateNamenode(NULL_CONTROLLER, req)
            .getRegistration()));
  }

  @Override
  public NamenodeCommand startCheckpoint(NamenodeRegistration registration)
      throws IOException {
    StartCheckpointRequestProto req = StartCheckpointRequestProto.newBuilder()
        .setRegistration(PBHelper.convert(registration)).build();
    NamenodeCommandProto cmd;
    cmd = ipc(() -> rpcProxy.startCheckpoint(NULL_CONTROLLER, req).getCommand());
    return PBHelper.convert(cmd);
  }

  @Override
  public void endCheckpoint(NamenodeRegistration registration,
      CheckpointSignature sig) throws IOException {
    EndCheckpointRequestProto req = EndCheckpointRequestProto.newBuilder()
        .setRegistration(PBHelper.convert(registration))
        .setSignature(PBHelper.convert(sig)).build();
    ipc(() -> rpcProxy.endCheckpoint(NULL_CONTROLLER, req));
  }

  @Override
  public RemoteEditLogManifest getEditLogManifest(long sinceTxId)
      throws IOException {
    GetEditLogManifestRequestProto req = GetEditLogManifestRequestProto
        .newBuilder().setSinceTxId(sinceTxId).build();
    return PBHelper.convert(ipc(() -> rpcProxy.getEditLogManifest(NULL_CONTROLLER, req)
        .getManifest()));
  }

  @Override
  public boolean isMethodSupported(String methodName) throws IOException {
    return RpcClientUtil.isMethodSupported(rpcProxy, NamenodeProtocolPB.class,
        RPC.RpcKind.RPC_PROTOCOL_BUFFER,
        RPC.getProtocolVersion(NamenodeProtocolPB.class), methodName);
  }

  @Override
  public boolean isUpgradeFinalized() throws IOException {
    IsUpgradeFinalizedRequestProto req = IsUpgradeFinalizedRequestProto
        .newBuilder().build();
    IsUpgradeFinalizedResponseProto response = ipc(() -> rpcProxy.isUpgradeFinalized(
        NULL_CONTROLLER, req));
    return response.getIsUpgradeFinalized();
  }

  @Override
  public boolean isRollingUpgrade() throws IOException {
    IsRollingUpgradeRequestProto req = IsRollingUpgradeRequestProto
        .newBuilder().build();
    IsRollingUpgradeResponseProto response = ipc(() -> rpcProxy.isRollingUpgrade(
        NULL_CONTROLLER, req));
    return response.getIsRollingUpgrade();
  }

  @Override
  public Long getNextSPSPath() throws IOException {
    GetNextSPSPathRequestProto req =
        GetNextSPSPathRequestProto.newBuilder().build();
    GetNextSPSPathResponseProto nextSPSPath =
        ipc(() -> rpcProxy.getNextSPSPath(NULL_CONTROLLER, req));
    return nextSPSPath.hasSpsPath() ? nextSPSPath.getSpsPath() : null;
  }
}
