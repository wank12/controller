

package org.opendaylight.controller.config.yang.test.impl;

import com.google.common.collect.Lists;

import java.util.List;

public class NetconfTestImplModuleUtil {
    static NetconfTestImplRuntimeRegistration registerRuntimeBeans(final NetconfTestImplModule module) {
        NetconfTestImplRuntimeRegistration reg = module.getRootRuntimeBeanRegistratorWrapper().register(new NetconfTestImplRuntimeMXBean() {

            @Override
            public Long getCreatedSessions() {
                return module.getSimpleLong();
            }

            @Override
            public Asdf getAsdf() {
                final Asdf asdf = new Asdf();
                asdf.setSimpleString("asdf");
                return asdf;
            }

            @Override
            public String noArg(final String arg1) {
                return arg1.toUpperCase();
            }

        });

        for (int i = 0; i < module.getSimpleShort(); i++) {
            final int finalI = i;

            reg.register(new InnerRunningDataAdditionalRuntimeMXBean() {
                @Override
                public Integer getSimpleInt3() {
                    return module.getSimpleTest();
                }

                @Override
                public Deep4 getDeep4() {
                    final Deep4 d = new Deep4();
                    d.setBoool(false);
                    return d;
                }

                @Override
                public String getSimpleString() {
                    return Integer.toString(finalI);
                }

                @Override
                public void noArgInner() {
                }
            });

            InnerRunningDataRuntimeRegistration innerReg = reg.register(new InnerRunningDataRuntimeMXBean() {
                @Override
                public Integer getSimpleInt3() {
                    return finalI;
                }

                @Override
                public Deep2 getDeep2() {
                    return new Deep2();
                }
            });

            for (int j = 0; j < module.getSimpleShort(); j++) {
                final int finalJ = j;
                innerReg.register(new InnerInnerRunningDataRuntimeMXBean() {
                    @Override
                    public List<NotStateBean> getNotStateBean() {
                        NotStateBean b1 = new NotStateBean();
                        b1.setElement("not state");
                        return Lists.newArrayList(b1);
                    }

                    @Override
                    public Integer getSimpleInt3() {
                        return finalJ;
                    }

                    @Override
                    public Deep3 getDeep3() {
                        return new Deep3();
                    }

                    @Override
                    public List<String> getListOfStrings() {
                        return Lists.newArrayList("l1", "l2");
                    }

                    @Override
                    public List<RetValList> listOutput() {
                        return Lists.newArrayList(new RetValList());
                    }

                    @Override
                    public Boolean noArgInnerInner(Integer integer, Boolean aBoolean) {
                        return aBoolean;
                    }

                    @Override
                    public RetValContainer containerOutput() {
                        return new RetValContainer();
                    }

                    @Override
                    public List<String> leafListOutput() {
                        return Lists.newArrayList("1", "2");
                    }
                });
            }
        }

        return reg;
    }
}
