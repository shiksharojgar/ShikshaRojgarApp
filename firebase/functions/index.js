const { onSchedule } = require('firebase-functions/v2/scheduler');
const { onDocumentCreated, onDocumentDeleted } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');
const { XMLParser } = require('fast-xml-parser');
const crypto = require('crypto');

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();
const parser = new XMLParser({ ignoreAttributes: false });

const FEEDS = [
  { source: 'Shiksha Rojgar', url: 'https://www.shiksharojgar.com/feeds/posts/default?alt=rss&max-results=5' },
  { source: 'VacancyBazaar', url: 'https://www.vacancybazaar.in/feeds/posts/default?alt=rss&max-results=5' }
];
function normalizeItems(parsed) { const channel=parsed?.rss?.channel||{}; const raw=channel.item||[]; return Array.isArray(raw)?raw:[raw]; }
function hashPost(p) { return crypto.createHash('sha256').update(`${p.title}|${p.link}|${p.pubDate||''}`).digest('hex'); }

exports.checkNewPosts = onSchedule({schedule:'every 15 minutes',timeZone:'Asia/Kolkata',region:'asia-south1'}, async()=>{
  for(const feed of FEEDS){
    try{
      const res=await fetch(feed.url,{headers:{'User-Agent':'ShikshaRojgarApp/1.5'}}); if(!res.ok)continue;
      const parsed=parser.parse(await res.text()); const items=normalizeItems(parsed).filter(x=>x&&x.title&&x.link); if(!items.length)continue;
      const newest=items[0], id=hashPost(newest), ref=db.collection('feed_state').doc(feed.source.replace(/\W/g,'_')), old=await ref.get();
      if(old.exists&&old.data().lastId===id)continue;
      await ref.set({lastId:id,title:newest.title,link:newest.link,checkedAt:admin.firestore.FieldValue.serverTimestamp()});
      if(old.exists)await messaging.send({topic:'all_updates',notification:{title:`${feed.source} – नई पोस्ट`,body:newest.title.slice(0,120)},data:{url:newest.link,source:feed.source,type:'new_post'},android:{priority:'high',notification:{channelId:'shiksha_updates'}}});
    }catch(e){console.error(feed.source,e);}
  }
});

exports.onChannelPostCreated = onDocumentCreated('channel_posts/{postId}', async(event)=>{
  const p=event.data?.data(); if(!p)return;
  await messaging.send({topic:'all_channel_followers',data:{postId:event.params.postId,type:'channel_post',title:`📢 ${p.category||'शिक्षा रोजगार चैनल'}`,body:String(p.title||'नई सूचना').slice(0,120)},android:{priority:'high'}});
});




exports.repairChannelCounters = onSchedule({schedule:'every 60 minutes',timeZone:'Asia/Kolkata',region:'asia-south1'}, async()=>{
  const followerSnap = await db.collection('channel_followers').count().get();
  const postsSnap = await db.collection('channel_posts').get();
  let totalViews = 0, totalShares = 0, totalLikes = 0, totalComments = 0;
  for (const post of postsSnap.docs) {
    const id = post.id;
    const [views, shares, likes, comments] = await Promise.all([
      db.collection('post_views').doc(id).collection('users').count().get(),
      db.collection('post_shares').doc(id).collection('users').count().get(),
      db.collection('channel_likes').doc(id).collection('users').count().get(),
      db.collection('channel_posts').doc(id).collection('comments').count().get()
    ]);
    const v=views.data().count||0, s=shares.data().count||0, l=likes.data().count||0, c=comments.data().count||0;
    totalViews += v; totalShares += s; totalLikes += l; totalComments += c;
    await post.ref.set({viewCount:v,shareCount:s,likeCount:l,commentCount:c},{merge:true});
  }
  await db.doc('channel_config/main').set({
    followerCount:followerSnap.data().count||0,
    totalPostViews:totalViews,
    totalPostShares:totalShares,
    totalLikes:totalLikes,
    totalComments:totalComments,
    countersRepairedAt:admin.firestore.FieldValue.serverTimestamp()
  },{merge:true});
  const [installs, usage] = await Promise.all([
    db.collection('app_installs').count().get(),
    db.collectionGroup('days').count().get()
  ]);
  // Feed view/share totals are maintained by their create triggers; app install/use
  // counters are also maintained by triggers. Do not overwrite them from a mixed
  // collectionGroup count because multiple features share a `users` subcollection.
  await db.doc('app_analytics/main').set({
    installUsers:installs.data().count||0,
    activeUserDays:usage.data().count||0,
    repairedAt:admin.firestore.FieldValue.serverTimestamp()
  },{merge:true});
});

exports.onFeedPostViewCreated = onDocumentCreated('feed_post_views/{postId}/users/{uid}', async()=>{
  await db.doc('app_analytics/main').set({feedPostViews:admin.firestore.FieldValue.increment(1)},{merge:true});
});
exports.onFeedPostShareCreated = onDocumentCreated('feed_post_shares/{postId}/users/{uid}', async()=>{
  await db.doc('app_analytics/main').set({feedPostShares:admin.firestore.FieldValue.increment(1)},{merge:true});
});
exports.onAppInstallCreated = onDocumentCreated('app_installs/{uid}', async()=>{
  await db.doc('app_analytics/main').set({installUsers:admin.firestore.FieldValue.increment(1)},{merge:true});
});
exports.onAppUsageCreated = onDocumentCreated('app_usage/{uid}/days/{day}', async()=>{
  await db.doc('app_analytics/main').set({activeUserDays:admin.firestore.FieldValue.increment(1)},{merge:true});
});
