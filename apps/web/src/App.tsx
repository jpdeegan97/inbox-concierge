import { useState, useEffect } from 'react';
import { 
  Inbox, Archive, Search, Bell, ChevronDown, 
  Sparkles, Bot, Command
} from 'lucide-react';
import './App.css';

interface Bucket {
  id: string;
  name: string;
  description: string;
  priority?: number;
  isDefault: boolean;
  createdByUser: boolean;
}

interface Email {
  threadId: string;
  sender: string;
  subject: string;
  preview: string;
  time: string;
  unread: boolean;
  classification: {
    bucketId: string;
    confidence: number;
    reason?: string;
  };
  tags: { label: string; urgent?: boolean }[];
}

interface InboxGroup {
  bucketId: string;
  bucketName: string;
  threads: Email[];
}

function App() {
  const [activeTab, setActiveTab] = useState('inbox');
  const [activeEmail, setActiveEmail] = useState<string | null>(null);

  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState<{ displayName: string; email: string } | null>(null);
  const [groups, setGroups] = useState<InboxGroup[]>([]);
  const [buckets, setBuckets] = useState<Bucket[]>([]);
  const [showAddBucket, setShowAddBucket] = useState(false);
  const [newBucketName, setNewBucketName] = useState('');

  const loadInbox = () => {
    setLoading(true);
    fetch('http://localhost:8080/api/inbox/load', {
      method: 'POST',
      credentials: 'include'
    })
      .then(res => {
        if (res.status === 401 || res.status === 403) throw new Error('Not authenticated');
        return res.json();
      })
      .then(data => {
        const fetchBuckets = data.buckets || [];
        const fetchGroups = data.groups || [];
        setBuckets(fetchBuckets);
        setGroups(fetchGroups);
        if (fetchGroups.length > 0 && fetchGroups[0].threads.length > 0) {
          setActiveEmail(fetchGroups[0].threads[0].threadId);
        }
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
      });
  };

  useEffect(() => {
    // Check session first
    fetch('http://localhost:8080/api/session', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        if (data.authenticated) {
          setAuthenticated(true);
          setUser(data.user);
          loadInbox();
        } else {
          setLoading(false);
        }
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  const handleLogin = () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  const handleAddBucket = () => {
    if (!newBucketName.trim()) return;
    setLoading(true);
    const newBucket = {
      id: '',
      name: newBucketName,
      description: '',
      isDefault: false,
      createdByUser: true
    };
    
    fetch('http://localhost:8080/api/inbox/reclassify', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ buckets: [...buckets, newBucket] })
    })
      .then(res => {
        if (!res.ok) throw new Error('API failed');
        return res.json();
      })
      .then(data => {
        setBuckets(data.buckets);
        setGroups(data.groups);
        setNewBucketName('');
        setShowAddBucket(false);
      })
      .catch(err => {
        console.error("Reclassification failed", err);
        alert("Failed to reclassify inbox. Check console details.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const getCombinedEmails = () => {
    if (!groups) return [];
    return groups.flatMap(group => group.threads || []);
  };

  return (
    <div className="app-container">
      {!authenticated ? (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh', width: '100vw' }}>
          <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', maxWidth: '400px' }}>
            <Sparkles size={48} color="var(--accent-primary)" style={{ marginBottom: '1rem' }} />
            <h1 style={{ color: 'white', marginBottom: '0.5rem' }}>Inbox Concierge</h1>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>Sign in to connect GSuite & Gmail</p>
            <button 
              onClick={handleLogin}
              style={{ width: '100%', padding: '1rem', background: 'white', color: 'black', border: 'none', borderRadius: '8px', fontWeight: 'bold', fontSize: '1rem', cursor: 'pointer' }}
            >
              Connect to Google
            </button>
          </div>
        </div>
      ) : (
        <>
          {/* Sidebar */}
          <aside className="sidebar glass-panel">
        <div className="brand">
          <div className="brand-icon">
            <Sparkles size={20} color="white" />
          </div>
          <div className="brand-name">Concierge</div>
        </div>

        <nav className="nav-group">
          <div className="nav-label">Favorites</div>
          <button className={`nav-item ${activeTab === 'inbox' ? 'active' : ''}`} onClick={() => setActiveTab('inbox')}>
            <div className="nav-item-left">
              <Inbox size={18} />
              <span>Inbox</span>
            </div>
            <span className="badge">{getCombinedEmails().length}</span>
          </button>
        </nav>

        <nav className="nav-group" style={{ marginTop: '1rem' }}>
          <div className="nav-label" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            Buckets
            <button onClick={() => setShowAddBucket(true)} style={{ color: 'var(--accent-primary)', fontSize: '1rem', fontWeight: 'bold' }}>+</button>
          </div>
          {buckets.map(b => {
            const count = groups.find(g => g.bucketId === b.id)?.threads.length || 0;
            return (
              <button 
                key={b.id} 
                className={`nav-item ${activeTab === b.id ? 'active' : ''}`}
                onClick={() => setActiveTab(b.id)}
              >
                <div className="nav-item-left">
                  <Archive size={18} />
                  <span>{b.name}</span>
                </div>
                <span className="badge">{count}</span>
              </button>
            );
          })}
        </nav>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        <header className="header glass-panel">
          <div className="search-bar">
            <Search size={18} color="var(--text-secondary)" />
            <input type="text" placeholder="Ask AI or search emails (Cmd + K)" />
            <Command size={14} color="var(--text-secondary)" />
          </div>

          <div className="header-actions">
            <button className="icon-button">
              <Bell size={18} />
            </button>
            <button className="profile-btn" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <div className="avatar" style={{ background: 'var(--accent-gradient)', borderRadius: '50%', width: '24px', height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '12px' }}>
                {user?.displayName ? user.displayName.charAt(0).toUpperCase() : 'U'}
              </div>
              <span style={{ fontSize: '0.8rem', color: 'white' }}>{user?.displayName}</span>
              <ChevronDown size={14} color="var(--text-secondary)" />
            </button>
          </div>
        </header>

        <div className="content-area">
          <div className="email-list-container glass-panel">
            <div className="list-header">
              <h2 className="list-header-title">Priority Inbox</h2>
              <div className="list-filters">
                <button className="filter-btn active">All</button>
                <button className="filter-btn">Action Needed</button>
              </div>
            </div>

            <div className={`email-list ${loading ? 'loading-blur' : ''}`}>
              {loading && (
                <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--accent-primary)', fontWeight: 'bold' }}>
                  <Sparkles size={16} className="spin-slow" style={{ marginRight: '0.5rem' }} /> 
                  Reclassifying Emails...
                </div>
              )}
              {groups
                  .filter(group => activeTab === 'inbox' || group.bucketId === activeTab)
                  .map(group => (
                <div key={group.bucketId}>
                  <h3 style={{ margin: '1rem 0.5rem 0.5rem', fontSize: '0.8rem', textTransform: 'uppercase', color: 'var(--accent-primary)', letterSpacing: '0.05em' }}>
                    {group.bucketName}
                  </h3>
                  {group.threads.map(email => (
                    <div 
                      key={email.threadId} 
                      className={`email-item ${email.unread ? 'unread' : ''} ${activeEmail === email.threadId ? 'active' : ''}`}
                      onClick={() => setActiveEmail(email.threadId)}
                    >
                      <div className="email-meta">
                        <span className="email-sender">{email.sender}</span>
                        <span className="email-time">{email.time}</span>
                      </div>
                      <div className="email-subject">{email.subject}</div>
                      <div className="email-preview">{email.preview}</div>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>

          <div className="concierge-panel glass-panel">
            {showAddBucket ? (
              <div style={{ padding: '2rem' }}>
                <h3 style={{ color: 'white', marginBottom: '1rem' }}>Create Custom Bucket</h3>
                <input 
                  type="text" 
                  value={newBucketName}
                  onChange={e => setNewBucketName(e.target.value)}
                  placeholder="E.g., Recruiters, Bills"
                  style={{ width: '100%', padding: '0.75rem', background: 'rgba(0,0,0,0.3)', border: '1px solid var(--border-color)', borderRadius: '8px', color: 'white', marginBottom: '1rem' }}
                />
                <button 
                  onClick={handleAddBucket}
                  disabled={loading}
                  className="btn btn-primary"
                  style={{ width: '100%', padding: '0.85rem', marginTop: '0.5rem', opacity: loading ? 0.7 : 1 }}
                >
                  {loading ? (
                    <>
                      <Sparkles size={16} className="spin-slow" /> Reclassifying...
                    </>
                  ) : 'Create & Reclassify'}
                </button>
                <button 
                  onClick={() => setShowAddBucket(false)}
                  disabled={loading}
                  className="btn btn-secondary"
                  style={{ width: '100%', padding: '0.85rem', marginTop: '0.5rem', background: 'transparent' }}
                >
                  Cancel
                </button>
              </div>
            ) : (
              <>
                <div className="concierge-header">
                  <div className="bot-avatar">
                    <Bot size={24} color="white" />
                  </div>
                  <div className="concierge-title-group">
                    <h2 className="text-gradient">AI Assistant</h2>
                    <p>Ready to help</p>
                  </div>
                </div>

                <div className="concierge-chat">
                   <div className="message message-bot">
                    <div className="msg-avatar"><Sparkles size={14}/></div>
                    <div className="msg-bubble">
                      I have automatically categorized your latest emails. Use the "+" button in the sidebar to define custom rules!
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </main>
      </>
      )}
    </div>
  );
}

export default App;
