// login.tsx

import React from 'react';
import Head from 'next/head';
import styles from '../styles/login.module.css'; // Adjust the path as necessary

const LoginPage: React.FC = () => {
    return (
        <div className={styles.container}>
            <Head>
                <title>Login</title>
                <meta name="description" content="Login Page" />
                <link rel="icon" href="/favicon.ico" />
            </Head>

            <nav className={styles.navbar}>
                {/* Navbar content here */}
            </nav>

            <div className={styles.loginBox}>
                <h1 className={styles.loginTitle}>Login</h1>
                <form>
                    <div className={styles.inputGroup}>
                        <label htmlFor="email">Email:</label>
                        <input type="email" id="email" name="email" required />
                    </div>
                    <div className={styles.inputGroup}>
                        <label htmlFor="password">Password:</label>
                        <input type="password" id="password" name="password" required />
                    </div>
                    <button type="submit" className={styles.loginButton}>Login</button>
                </form>
            </div>
        </div>
    );
};

export default LoginPage;
