package com.cmpay.lemon.monitor.utils;

import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;


public class SVNUtil {
    private static Logger logger = Logger.getLogger(SVNUtil.class);

    /**
     * 通过不同的协议初始化版本库
     */
    public static void setupLibrary() {
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }

    /**
     * 验证登录svn
     * @Param: svnRoot:svn的根路径
     */
    public static SVNClientManager authSvn(String svnRoot, ISVNAuthenticationManager authManager ) {
        // 初始化版本库
        setupLibrary();

        // 创建库连接
        SVNRepository repository = null;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(svnRoot));
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(),e);
            return null;
        }
        // 创建身份验证管理器
        repository.setAuthenticationManager(authManager);

        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        SVNClientManager clientManager = SVNClientManager.newInstance(options, authManager);
        return clientManager;
    }

    /**
     * 创建文件夹
     */
    public static SVNCommitInfo makeDirectory(SVNClientManager clientManager, SVNURL url, String commitMessage) {
        try {
              return  clientManager.getCommitClient().doMkDir(new SVNURL[]{url}, commitMessage);
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return null;
    }
    /**
     * 修改文件夹
     */
    public static void modifyFolder(SVNClientManager clientManager, SVNURL oldUrl, SVNURL newUrl,File file) {



        try {
            SVNURL urlDir = SVNURL.parseURIEncoded("http://10.9.10.115:18080/svn/Projectdoc/209902");
            SVNUtil.makeDirectory(clientManager,urlDir,"REQ-20990213-2190_测试2233");
             SVNURL urlDir1 = SVNURL.parseURIEncoded("http://10.9.10.115:18080/svn/Projectdoc/209902/REQ-20990213-2190_测试");
            SVNURL urlDir2 = SVNURL.parseURIEncoded("http://10.9.10.115:18080/svn/Projectdoc/209902/REQ-20990213-2190_测试2233");
            clientManager.getUpdateClient().doRelocate(file,urlDir1,urlDir2,true);
            return ;
        } catch (SVNException e) {
            System.err.println(333);
            logger.error(e.getErrorMessage(), e);
        }
        return ;
    }
    
   
    /**
     * 导入文件夹
     * @Param localPath:本地路径
     * @Param dstURL:目标地址
     */
    public static SVNCommitInfo importDirectory(SVNClientManager clientManager, File localPath, SVNURL dstURL, String commitMessage, boolean isRecursive) {
        try {
            return clientManager.getCommitClient().doImport(localPath, dstURL, commitMessage, null, true, true, SVNDepth.fromRecurse(isRecursive));
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return null;
    }

    /**
     * 添加入口
     */
    public static void addEntry(SVNClientManager clientManager, File wcPath) {
        try {
            clientManager.getWCClient().doAdd(new File[]{wcPath}, true, false, false, SVNDepth.INFINITY, false, false, true);
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(),e);
        }
    }

    /**
     * 显示状态
     */
    public static SVNStatus showStatus(SVNClientManager clientManager, File wcPath, boolean remote) {
        SVNStatus status = null;
        try {
            status = clientManager.getStatusClient().doStatus(wcPath, remote);
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return status;
    }

    /**
     * 提交
     * @Param keepLocks:是否保持锁定
     */
    public static SVNCommitInfo commit(SVNClientManager clientManager, File wcPath, boolean keepLocks, String commitMessage) {
        try {
            return clientManager.getCommitClient().doCommit(new File[]{wcPath}, keepLocks, commitMessage, null, null, false, false, SVNDepth.INFINITY);
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return null;
    }

    /**
     * 更新
     */
    public static long update(SVNClientManager clientManager, File wcPath, SVNRevision updateToRevision, SVNDepth depth) {
        SVNUpdateClient updateClient = clientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        try {
            return updateClient.doUpdate(wcPath, updateToRevision, depth, false, false);
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return 0;
    }

    /**
     * 从SVN导出项目到本地
     * @Param url:SVN的url
     * @Param revision:版本
     * @Param destPath:目标路径
     */
    public static long checkout(SVNClientManager clientManager, SVNURL url, SVNRevision revision, File destPath, SVNDepth depth) {
        SVNUpdateClient updateClient = clientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        try {
            return updateClient.doCheckout(url, destPath, revision, revision, depth, false);
        } catch(SVNException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return 0;
    }

    /**
     * 确定path是否是一个工作空间
     */
    public static boolean isWorkingCopy(File path) {
        if(!path.exists()) {
            logger.warn("'" + path + "' not exist!");
            return false;
        }
        try {
            if(null == SVNWCUtil.getWorkingCopyRoot(path, false)) {
                return false;
            }
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return true;
    }

    /**
     * 确定一个URL在SVN上是否存在
     */
    public static boolean isURLExist(SVNURL url, ISVNAuthenticationManager authManager) {
        try {
            SVNRepository svnRepository = SVNRepositoryFactory.create(url);
            svnRepository.setAuthenticationManager(authManager);
            SVNNodeKind nodeKind = svnRepository.checkPath("", -1); //遍历SVN,获取结点。
            return nodeKind == SVNNodeKind.NONE ? false : true;
        } catch (SVNException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return false;
    }
    
    /**  
     * 递归检查不在版本控制的文件，并add到svn  
     * @param wc  检查的文件
     * @throws SVNException 异常信息
     */  
    public static void checkVersiondDirectory(SVNClientManager clientManager, File wc) throws SVNException {
        if(!SVNWCUtil.isVersionedDirectory(wc)){
            addEntry(clientManager,wc);  
        }  
        if(wc.isDirectory()){  
            for(File sub:wc.listFiles()){
                if(sub.isDirectory() && sub.getName().equals(".svn")){
                    continue;  
                }  
                checkVersiondDirectory(clientManager,sub);  
            }  
        }  
    } 
    
}
