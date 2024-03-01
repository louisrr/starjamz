import React from 'react';
import Head from 'next/head';
import styles from '../styles/signup.module.css'; // Adjust the path as necessary

const SignupPage: React.FC = () => {
    return (
        <div className={styles.container}>
            <Head>
                <title>Signup</title>
                <meta name="description" content="Signup Page" />
                <link rel="icon" href="/favicon.ico" />
            </Head>

            <nav className={styles.navbar}>
                {/* Navbar content here */}
            </nav>

            <div className={styles.signupBox}>
                <form>
                    <div className={styles.inputGroup}>
                        <label htmlFor="email">Email Address or Phone Number:</label>
                        <input type="text" id="email" name="email" required />
                    </div>
                    <div className={styles.inputGroup}>
                        <label htmlFor="confirmEmail">Confirm Email Address or Phone Number:</label>
                        <input type="text" id="confirmEmail" name="confirmEmail" required />
                    </div>
                    <div className={styles.inputGroup}>
                        <label htmlFor="password">Password:</label>
                        <input type="password" id="password" name="password" required />
                    </div>
                    <button type="submit" className={styles.submitButton}>Submit</button>
                </form>
            </div>
        </div>
    );
};

export default SignupPage;
